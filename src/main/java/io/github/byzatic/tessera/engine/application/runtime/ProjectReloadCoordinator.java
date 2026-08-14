package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.lib.configio.application.revision.ProjectRevision;
import io.github.byzatic.lib.configio.application.revision.ProjectRevisionFailure;
import io.github.byzatic.lib.configio.application.revision.ProjectRevisionListener;
import io.github.byzatic.lib.configio.application.revision.ProjectRevisionSource;
import io.github.byzatic.lib.configio.domain.exception.ProjectRevisionException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serializes project revision changes and replaces complete project runtimes atomically.
 *
 * <p>All mutable runtime state is confined to a single executor thread. External callbacks
 * only enqueue commands and never mutate the active runtime directly.</p>
 */
public final class ProjectReloadCoordinator
        implements ProjectRevisionListener, AutoCloseable {

    private static final Logger logger =
            LoggerFactory.getLogger(ProjectReloadCoordinator.class);

    private final ProjectRevisionSource revisionSource;
    private final ProjectRuntimeFactory runtimeFactory;
    private final Duration startupTimeout;
    private final Duration shutdownTimeout;
    private final ExecutorService reloadExecutor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean revisionDrainScheduled = new AtomicBoolean(false);
    private final AtomicReference<ProjectRevision> pendingRevision =
            new AtomicReference<ProjectRevision>();
    private final CountDownLatch terminated = new CountDownLatch(1);

    private ProjectRuntime activeRuntime;
    private ProjectRevision activeRevision;

    public ProjectReloadCoordinator(
            ProjectRevisionSource revisionSource,
            ProjectRuntimeFactory runtimeFactory,
            Duration startupTimeout,
            Duration shutdownTimeout
    ) {
        this.revisionSource = Objects.requireNonNull(revisionSource, "revisionSource");
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
        this.startupTimeout = Objects.requireNonNull(startupTimeout, "startupTimeout");
        this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
        this.reloadExecutor = Executors.newSingleThreadExecutor(new ReloadThreadFactory());
    }

    /**
     * Starts project revision observation.
     *
     * @throws ProjectRevisionException when the source cannot be started
     */
    public void start() throws ProjectRevisionException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Project reload coordinator is already started");
        }
        revisionSource.start(this);
    }

    /**
     * Blocks until the coordinator has been closed.
     *
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public void awaitTermination() throws InterruptedException {
        terminated.await();
    }

    @Override
    public void onRevisionAvailable(final ProjectRevision revision) {
        Objects.requireNonNull(revision, "revision");
        if (closed.get()) {
            closeRevision(revision, null);
            return;
        }
        ProjectRevision superseded = pendingRevision.getAndSet(revision);
        closeRevision(superseded, null);
        scheduleRevisionDrain();
    }

    private void scheduleRevisionDrain() {
        if (!revisionDrainScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            reloadExecutor.execute(new RevisionDrainCommand());
        } catch (RejectedExecutionException exception) {
            revisionDrainScheduled.set(false);
            ProjectRevision rejected = pendingRevision.getAndSet(null);
            closeRevision(rejected, exception);
        }
    }

    @Override
    public void onRevisionRejected(ProjectRevisionFailure failure) {
        logger.error(
                "Project archive revision {} was rejected",
                failure.getRevisionId(),
                failure.getCause()
        );
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        revisionSource.close();
        reloadExecutor.shutdown();
        try {
            if (!reloadExecutor.awaitTermination(
                    shutdownTimeout.toMillis(),
                    TimeUnit.MILLISECONDS)) {
                reloadExecutor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            reloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        stopAndCloseActiveRuntime();
        closeRevision(pendingRevision.getAndSet(null), null);
        terminated.countDown();
    }

    private void activate(ProjectRevision revision) {
        if (activeRevision != null
                && activeRevision.getRevisionId().equals(revision.getRevisionId())) {
            closeRevision(revision, null);
            return;
        }

        ProjectRuntime candidate;
        try {
            candidate = runtimeFactory.create(revision);
        } catch (OperationIncompleteException exception) {
            logger.error("Cannot prepare project revision {}", revision.getRevisionId(), exception);
            closeRevision(revision, exception);
            return;
        }

        if (activeRuntime == null) {
            startInitialRuntime(candidate, revision);
            return;
        }

        ProjectRuntime previousRuntime = activeRuntime;
        ProjectRevision previousRevision = activeRevision;
        activeRuntime = null;
        activeRevision = null;

        previousRuntime.stop(shutdownTimeout);
        try {
            candidate.start(startupTimeout);
            activeRuntime = candidate;
            activeRevision = revision;
            closeRuntime(previousRuntime);
            closeRevision(previousRevision, null);
            logger.info("Activated project revision {}", revision.getRevisionId());
        } catch (OperationIncompleteException exception) {
            logger.error(
                    "Cannot start project revision {}; rolling back to {}",
                    revision.getRevisionId(),
                    previousRevision.getRevisionId(),
                    exception
            );
            candidate.stop(shutdownTimeout);
            closeRuntime(candidate);
            closeRevision(revision, exception);
            rollback(previousRuntime, previousRevision, exception);
        }
    }

    private void startInitialRuntime(ProjectRuntime runtime, ProjectRevision revision) {
        try {
            runtime.start(startupTimeout);
            activeRuntime = runtime;
            activeRevision = revision;
            logger.info("Activated initial project revision {}", revision.getRevisionId());
        } catch (OperationIncompleteException exception) {
            logger.error("Cannot start initial project revision {}", revision.getRevisionId(), exception);
            runtime.stop(shutdownTimeout);
            closeRuntime(runtime);
            closeRevision(revision, exception);
        }
    }

    private void rollback(
            ProjectRuntime stoppedRuntime,
            ProjectRevision previousRevision,
            Throwable reloadFailure
    ) {
        closeRuntime(stoppedRuntime);
        try {
            ProjectRuntime rollbackRuntime = runtimeFactory.create(previousRevision);
            rollbackRuntime.start(startupTimeout);
            activeRuntime = rollbackRuntime;
            activeRevision = previousRevision;
            logger.info("Rollback to project revision {} completed", previousRevision.getRevisionId());
        } catch (Exception rollbackFailure) {
            reloadFailure.addSuppressed(rollbackFailure);
            logger.error(
                    "Rollback to project revision {} failed",
                    previousRevision.getRevisionId(),
                    rollbackFailure
            );
            closeRevision(previousRevision, rollbackFailure);
        }
    }

    private void stopAndCloseActiveRuntime() {
        ProjectRuntime runtime = activeRuntime;
        ProjectRevision revision = activeRevision;
        activeRuntime = null;
        activeRevision = null;
        if (runtime != null) {
            runtime.stop(shutdownTimeout);
            closeRuntime(runtime);
        }
        closeRevision(revision, null);
    }

    private void closeRuntime(ProjectRuntime runtime) {
        try {
            runtime.close();
        } catch (RuntimeException exception) {
            logger.error("Cannot close project runtime {}", runtime.getRevisionId(), exception);
        }
    }

    private void closeRevision(ProjectRevision revision, Throwable failure) {
        if (revision == null) {
            return;
        }
        try {
            revision.close();
        } catch (IOException exception) {
            if (failure != null) {
                failure.addSuppressed(exception);
            }
            logger.error("Cannot close project revision {}", revision.getRevisionId(), exception);
        }
    }

    private final class RevisionDrainCommand implements Runnable {
        @Override
        public void run() {
            ProjectRevision revision;
            while ((revision = pendingRevision.getAndSet(null)) != null) {
                if (closed.get()) {
                    closeRevision(revision, null);
                } else {
                    activate(revision);
                }
            }
            revisionDrainScheduled.set(false);
            if (pendingRevision.get() != null) {
                scheduleRevisionDrain();
            }
        }
    }

    private static final class ReloadThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "project-reload-coordinator");
            thread.setDaemon(false);
            return thread;
        }
    }
}
