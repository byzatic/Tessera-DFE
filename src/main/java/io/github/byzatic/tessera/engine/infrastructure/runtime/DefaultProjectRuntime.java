package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntime;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
    private final ModuleLoaderInterface moduleLoader;
    private final ServiceLoaderInterface serviceLoader;
    private final ExecutorService orchestrationExecutor;
    private final AtomicReference<Throwable> executionFailure = new AtomicReference<Throwable>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public DefaultProjectRuntime(
            String revisionId,
            OrchestrationServiceInterface orchestrationService,
            StorageManagerInterface storageManager,
            ModuleLoaderInterface moduleLoader,
            ServiceLoaderInterface serviceLoader
    ) {
        this.revisionId = Objects.requireNonNull(revisionId, "revisionId");
        this.orchestrationService = Objects.requireNonNull(
                orchestrationService,
                "orchestrationService"
        );
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.moduleLoader = Objects.requireNonNull(moduleLoader, "moduleLoader");
        this.serviceLoader = Objects.requireNonNull(serviceLoader, "serviceLoader");
        this.orchestrationExecutor = Executors.newSingleThreadExecutor(
                new OrchestrationThreadFactory(revisionId)
        );
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

        orchestrationExecutor.execute(new OrchestrationCommand());
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
        orchestrationExecutor.shutdown();
        awaitExecutor(shutdownTimeout);
        cleanupStorages();
        cleanupProjectMetrics();
        closeLoaders();
    }

    @Override
    public String getRevisionId() {
        return revisionId;
    }

    @Override
    public void close() {
        stop(DEFAULT_SHUTDOWN_TIMEOUT);
    }

    private void awaitExecutor(Duration shutdownTimeout) {
        try {
            if (!orchestrationExecutor.awaitTermination(
                    shutdownTimeout.toMillis(),
                    TimeUnit.MILLISECONDS)) {
                logger.warn("Forcing orchestration executor shutdown for revision {}", revisionId);
                orchestrationExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            orchestrationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void closeLoaders() {
        try {
            moduleLoader.close();
        } catch (Exception exception) {
            logger.error("Cannot close module loader for revision {}", revisionId, exception);
        }
        try {
            serviceLoader.close();
        } catch (Exception exception) {
            logger.error("Cannot close service loader for revision {}", revisionId, exception);
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
            }
        }
    }

    private static final class OrchestrationThreadFactory implements ThreadFactory {

        private final String revisionId;

        private OrchestrationThreadFactory(String revisionId) {
            this.revisionId = revisionId;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String shortRevision = revisionId.substring(0, Math.min(12, revisionId.length()));
            Thread thread = new Thread(runnable, "project-runtime-" + shortRevision);
            thread.setDaemon(false);
            return thread;
        }
    }
}
