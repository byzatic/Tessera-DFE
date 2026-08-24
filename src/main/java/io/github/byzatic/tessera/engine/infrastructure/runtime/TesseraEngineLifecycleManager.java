package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.commons.schedulers.immediate.CancellationToken;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.Task;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionWatchRequest;
import io.github.byzatic.tessera.lib.configio.unified.TesseraProjectIO;
import io.github.byzatic.tessera.lib.configio.unified.internal.DefaultTesseraProjectIO;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.runtime.ProjectReloadCoordinator;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntimeFactory;
import io.github.byzatic.tessera.engine.infrastructure.observability.PrometheusMetricsAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the engine-level lifecycle, background control task, and shutdown sequence.
 *
 * <p>This class is the composition root for infrastructure required by the running engine.
 * Project-specific dependency graphs remain the responsibility of {@link ProjectRuntimeFactory}.</p>
 *
 * <p>The class is thread-safe. Startup is performed once by the calling thread, while shutdown
 * may be requested concurrently by the JVM shutdown hook or by the owner of this object.</p>
 */
public final class TesseraEngineLifecycleManager implements AutoCloseable {

    private static final Logger logger =
            LoggerFactory.getLogger(TesseraEngineLifecycleManager.class);

    private final ProjectReloadCoordinator reloadCoordinator;
    private final PrometheusMetricsAgent metricsAgent;
    private final ImmediateSchedulerInterface lifecycleScheduler;
    private final Duration shutdownTimeout;
    private final CountDownLatch lifecycleTerminated = new CountDownLatch(1);
    private final AtomicReference<Throwable> lifecycleFailure =
            new AtomicReference<Throwable>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile UUID lifecycleTaskId;

    private TesseraEngineLifecycleManager(
            ProjectReloadCoordinator reloadCoordinator,
            PrometheusMetricsAgent metricsAgent,
            ImmediateSchedulerInterface lifecycleScheduler,
            Duration shutdownTimeout
    ) {
        this.reloadCoordinator = Objects.requireNonNull(
                reloadCoordinator,
                "reloadCoordinator"
        );
        this.metricsAgent = Objects.requireNonNull(metricsAgent, "metricsAgent");
        this.lifecycleScheduler = Objects.requireNonNull(
                lifecycleScheduler,
                "lifecycleScheduler"
        );
        this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
    }

    /**
     * Creates the default engine lifecycle and all process-scoped infrastructure.
     *
     * @return lifecycle manager configured from {@link Configuration}
     */
    public static TesseraEngineLifecycleManager createDefault() {
        TesseraProjectIO projectIO = DefaultTesseraProjectIO.createDefault();
        ProjectRevisionWatchRequest watchRequest = createRevisionWatchRequest();
        ProjectRuntimeFactory runtimeFactory = new DefaultProjectRuntimeFactory();
        ProjectReloadCoordinator reloadCoordinator = new ProjectReloadCoordinator(
                projectIO,
                watchRequest,
                runtimeFactory,
                Configuration.PROJECT_INITIAL_REVISION_TIMEOUT,
                Configuration.PROJECT_STARTUP_TIMEOUT,
                Configuration.PROJECT_SHUTDOWN_TIMEOUT
        );
        ImmediateSchedulerInterface lifecycleScheduler =
                new ImmediateScheduler.Builder()
                        .defaultGrace(Configuration.PROJECT_SHUTDOWN_TIMEOUT)
                        .build();
        return new TesseraEngineLifecycleManager(
                reloadCoordinator,
                PrometheusMetricsAgent.getInstance(),
                lifecycleScheduler,
                Configuration.PROJECT_SHUTDOWN_TIMEOUT
        );
    }

    /**
     * Runs the engine until shutdown is requested or the lifecycle task fails.
     *
     * @throws Exception when startup fails, waiting is interrupted, or the lifecycle task fails
     */
    public void run() throws Exception {
        Thread shutdownHook = createShutdownHook();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            if (start()) {
                lifecycleTerminated.await();
                rethrowLifecycleFailure();
            }
        } finally {
            close();
            removeShutdownHook(shutdownHook);
        }
    }

    /**
     * Starts process metrics and submits the engine control task to the lifecycle scheduler.
     *
     * @throws Exception when metrics or lifecycle task startup fails
     */
    private synchronized boolean start() throws Exception {
        if (closed.get()) {
            return false;
        }
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Engine lifecycle is already started");
        }

        metricsAgent.start(Configuration.PROMETHEUS_URI);
        if (Configuration.JVM_METRICS_ENABLED) {
            metricsAgent.enableJvmMetrics();
        }

        lifecycleTaskId = lifecycleScheduler.addTask(new EngineLifecycleTask());
        if (closed.get()) {
            closeLifecycleTask();
        }
        return true;
    }

    /**
     * Stops revision observation, the active project runtime, metrics, and lifecycle scheduling.
     */
    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        reloadCoordinator.close();
        closeLifecycleTask();
        closeLifecycleScheduler();
        metricsAgent.stop();
        lifecycleTerminated.countDown();
    }

    /**
     * Builds ZIP observation settings from process configuration.
     *
     * @return unified project revision watch request
     */
    private static ProjectRevisionWatchRequest createRevisionWatchRequest() {
        return ProjectRevisionWatchRequest.builder(
                        Configuration.PROJECT_ARCHIVE_PATH,
                        Configuration.PROJECT_STAGING_DIRECTORY
                )
                .pollInterval(Configuration.PROJECT_WATCH_INTERVAL)
                .build();
    }

    /**
     * Creates the mandatory JVM shutdown hook delegating to the lifecycle owner.
     *
     * @return shutdown hook thread
     */
    private Thread createShutdownHook() {
        return new Thread(new ShutdownCommand(), "tessera-shutdown");
    }

    /**
     * Removes the shutdown hook after normal lifecycle completion.
     *
     * @param shutdownHook previously registered JVM shutdown hook
     */
    private void removeShutdownHook(Thread shutdownHook) {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            logger.debug("JVM shutdown is already in progress");
        }
    }

    /**
     * Requests cooperative cancellation of the scheduled lifecycle task.
     */
    private void closeLifecycleTask() {
        UUID currentTaskId = lifecycleTaskId;
        if (currentTaskId != null) {
            lifecycleScheduler.removeTask(currentTaskId, shutdownTimeout);
        }
    }

    /**
     * Closes the lifecycle scheduler without preventing the remaining shutdown sequence.
     */
    private void closeLifecycleScheduler() {
        try {
            lifecycleScheduler.close();
        } catch (Exception exception) {
            logger.error("Cannot close engine lifecycle scheduler", exception);
        }
    }

    /**
     * Propagates a background lifecycle failure to the thread running the engine.
     *
     * @throws Exception original checked lifecycle failure
     */
    private void rethrowLifecycleFailure() throws Exception {
        Throwable failure = lifecycleFailure.get();
        if (failure == null) {
            return;
        }
        if (failure instanceof Exception) {
            throw (Exception) failure;
        }
        throw new IllegalStateException("Engine lifecycle failed", failure);
    }

    private final class EngineLifecycleTask implements Task {

        @Override
        public void run(CancellationToken cancellationToken) throws Exception {
            try {
                cancellationToken.throwIfStopRequested();
                reloadCoordinator.start();
                cancellationToken.throwIfStopRequested();
                reloadCoordinator.awaitTermination();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                if (!closed.get()) {
                    lifecycleFailure.compareAndSet(null, exception);
                }
            } catch (Exception exception) {
                lifecycleFailure.compareAndSet(null, exception);
                throw exception;
            } finally {
                lifecycleTerminated.countDown();
            }
        }

        @Override
        public void onStopRequested() {
            reloadCoordinator.close();
        }
    }

    private final class ShutdownCommand implements Runnable {

        @Override
        public void run() {
            close();
        }
    }
}
