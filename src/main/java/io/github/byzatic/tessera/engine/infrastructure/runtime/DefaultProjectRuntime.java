package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.commons.schedulers.unified.RunHandle;
import io.github.byzatic.commons.schedulers.unified.UnifiedSchedulerInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntime;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntimeFailureListener;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Default isolated runtime for one project revision.
 */
public final class DefaultProjectRuntime implements ProjectRuntime {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProjectRuntime.class);
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofMinutes(3L);

    private final String revisionId;
    private final OrchestrationServiceInterface orchestrationService;
    private final StorageManagerInterface storageManager;
    private final UnifiedSchedulerInterface scheduler;
    private final AtomicReference<Throwable> executionFailure = new AtomicReference<Throwable>();
    private final AtomicReference<ProjectRuntimeFailureListener> failureListener =
            new AtomicReference<ProjectRuntimeFailureListener>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile RunHandle orchestrationRun;

    public DefaultProjectRuntime(
            String revisionId,
            OrchestrationServiceInterface orchestrationService,
            StorageManagerInterface storageManager,
            UnifiedSchedulerInterface scheduler
    ) {
        this.revisionId = Objects.requireNonNull(revisionId, "revisionId");
        this.orchestrationService = Objects.requireNonNull(
                orchestrationService,
                "orchestrationService"
        );
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void setFailureListener(ProjectRuntimeFailureListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!failureListener.compareAndSet(null, listener)) {
            throw new IllegalStateException(
                    "Project runtime failure listener is already registered: " + revisionId
            );
        }
    }

    @Override
    public void start(Duration startupTimeout) throws OperationIncompleteException {
        Objects.requireNonNull(startupTimeout, "startupTimeout");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Project runtime is already started: " + revisionId);
        }
        if (stopped.get()) {
            throw new IllegalStateException("Project runtime is already stopped: " + revisionId);
        }

        orchestrationRun = scheduler.submit(new OrchestrationCommand());
        try {
            if (!orchestrationService.awaitStarted(startupTimeout)) {
                Throwable failure = executionFailure.get();
                if (failure == null) {
                    throw new OperationIncompleteException(
                            "Project runtime did not reach RUNNING state: " + revisionId
                    );
                }
                throw new OperationIncompleteException(
                        "Project runtime failed during startup: " + revisionId,
                        failure
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OperationIncompleteException(
                    "Interrupted while starting project runtime: " + revisionId,
                    exception
            );
        }
    }

    @Override
    public void stop(Duration shutdownTimeout) {
        Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
        if (!stopped.compareAndSet(false, true)) {
            return;
        }

        orchestrationService.stop();
        awaitOrchestration(shutdownTimeout);
        cleanupStorages();
        cleanupProjectMetrics();
    }

    @Override
    public String getRevisionId() {
        return revisionId;
    }

    @Override
    public void close() {
        stop(DEFAULT_SHUTDOWN_TIMEOUT);
    }

    private void awaitOrchestration(Duration shutdownTimeout) {
        RunHandle run = orchestrationRun;
        if (run == null) {
            return;
        }
        try {
            run.await(shutdownTimeout);
        } catch (TimeoutException exception) {
            logger.warn("Forcing orchestration cancellation for revision {}", revisionId);
            try {
                run.cancel("Project runtime shutdown timed out", Duration.ZERO);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        } catch (ExecutionException exception) {
            logger.debug("Orchestration already failed for revision {}", revisionId, exception);
        } catch (InterruptedException exception) {
            run.requestCancellation("Project runtime shutdown interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void cleanupStorages() {
        try {
            storageManager.cleanupStorages();
        } catch (Exception exception) {
            logger.error("Cannot clean storages for revision {}", revisionId, exception);
        }
    }

    /**
     * Removes collectors registered by project plugins in the global Prometheus registry.
     * Engine metrics use a dedicated registry and are not affected by this cleanup.
     */
    private void cleanupProjectMetrics() {
        try {
            PrometheusRegistry.defaultRegistry.clear();
        } catch (RuntimeException exception) {
            logger.error("Cannot clean project metrics for revision {}", revisionId, exception);
        }
    }

    private final class OrchestrationCommand implements Runnable {
        @Override
        public void run() {
            try {
                orchestrationService.start();
            } catch (Throwable failure) {
                executionFailure.compareAndSet(null, failure);
                logger.error("Project runtime {} terminated with an error", revisionId, failure);
                notifyFailureListener(failure);
            }
        }
    }

    private void notifyFailureListener(Throwable failure) {
        ProjectRuntimeFailureListener listener = failureListener.get();
        if (listener == null || stopped.get()) {
            return;
        }
        try {
            listener.onFailure(this, failure);
        } catch (RuntimeException listenerFailure) {
            failure.addSuppressed(listenerFailure);
            logger.error(
                    "Cannot report fatal failure of project runtime {}",
                    revisionId,
                    listenerFailure
            );
        }
    }

}
