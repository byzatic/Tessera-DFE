package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.lib.configio.unified.ProjectRevisionError;
import io.github.byzatic.lib.configio.unified.ProjectRevisionHandle;
import io.github.byzatic.lib.configio.unified.ProjectRevisionListener;
import io.github.byzatic.lib.configio.unified.ProjectRevisionSubscription;
import io.github.byzatic.lib.configio.unified.ProjectRevisionWatchRequest;
import io.github.byzatic.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.lib.configio.unified.TesseraProjectIO;
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

    private final TesseraProjectIO projectIO;
    private final ProjectRevisionWatchRequest watchRequest;
    private final ProjectRuntimeFactory runtimeFactory;
    private final Duration startupTimeout;
    private final Duration shutdownTimeout;
    private final ExecutorService reloadExecutor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean revisionDrainScheduled = new AtomicBoolean(false);
    private final AtomicReference<ProjectRevisionHandle> pendingRevision =
            new AtomicReference<ProjectRevisionHandle>();
    private final AtomicReference<Throwable> terminalFailure =
            new AtomicReference<Throwable>();
    private final CountDownLatch terminated = new CountDownLatch(1);

    private volatile ProjectRevisionSubscription revisionSubscription;
    private ProjectRuntime activeRuntime;
    private ProjectRevisionHandle activeRevision;

    public ProjectReloadCoordinator(
            TesseraProjectIO projectIO,
            ProjectRevisionWatchRequest watchRequest,
            ProjectRuntimeFactory runtimeFactory,
            Duration startupTimeout,
            Duration shutdownTimeout
    ) {
        this.projectIO = Objects.requireNonNull(projectIO, "projectIO");
        this.watchRequest = Objects.requireNonNull(watchRequest, "watchRequest");
        this.runtimeFactory = Objects.requireNonNull(runtimeFactory, "runtimeFactory");
        this.startupTimeout = Objects.requireNonNull(startupTimeout, "startupTimeout");
        this.shutdownTimeout = Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");
        this.reloadExecutor = Executors.newSingleThreadExecutor(new ReloadThreadFactory());
    }

    /**
     * Starts project revision observation.
     *
     * @throws TesseraProjectException when revision observation cannot be started
     */
    public void start() throws TesseraProjectException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Project reload coordinator is already started");
        }
        ProjectRevisionSubscription subscription = projectIO.watchRevisions(watchRequest, this);
        revisionSubscription = subscription;
        if (closed.get()) {
            subscription.close();
        }
    }

    /**
     * Blocks until the coordinator has been closed.
     *
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public void awaitTermination()
            throws InterruptedException, OperationIncompleteException {
        terminated.await();
        Throwable failure = terminalFailure.get();
        if (failure != null) {
            throw new OperationIncompleteException(
                    "Active project runtime terminated with an error",
                    failure
            );
        }
    }

    @Override
    public void onRevisionAvailable(final ProjectRevisionHandle revision) {
        Objects.requireNonNull(revision, "revision");
        if (closed.get()) {
            closeRevision(revision, null);
            return;
        }
        ProjectRevisionHandle superseded = pendingRevision.getAndSet(revision);
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
            ProjectRevisionHandle rejected = pendingRevision.getAndSet(null);
            closeRevision(rejected, exception);
        }
    }

    @Override
    public void onRevisionRejected(ProjectRevisionError failure) {
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

        closeRevisionSubscription();
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

    private void activate(ProjectRevisionHandle revision) {
        if (activeRevision != null
                && activeRevision.getRevisionId().equals(revision.getRevisionId())) {
            closeRevision(revision, null);
            return;
        }

        ProjectRuntime candidate;
        try {
            candidate = runtimeFactory.create(revision);
            candidate.setFailureListener(new RuntimeFailureListener());
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
        ProjectRevisionHandle previousRevision = activeRevision;
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

    private void reportRuntimeFailure(ProjectRuntime runtime, Throwable failure) {
        if (closed.get()) {
            return;
        }
        try {
            reloadExecutor.execute(new RuntimeFailureCommand(runtime, failure));
        } catch (RejectedExecutionException exception) {
            failure.addSuppressed(exception);
            terminalFailure.compareAndSet(null, failure);
            terminated.countDown();
        }
    }

    private final class RuntimeFailureListener implements ProjectRuntimeFailureListener {

        @Override
        public void onFailure(ProjectRuntime runtime, Throwable failure) {
            reportRuntimeFailure(runtime, failure);
        }
    }

    private final class RuntimeFailureCommand implements Runnable {

        private final ProjectRuntime failedRuntime;
        private final Throwable failure;

        private RuntimeFailureCommand(ProjectRuntime failedRuntime, Throwable failure) {
            this.failedRuntime = failedRuntime;
            this.failure = failure;
        }

        @Override
        public void run() {
            if (failedRuntime != activeRuntime) {
                return;
            }
            if (!terminalFailure.compareAndSet(null, failure)) {
                return;
            }

            logger.error(
                    "Active project runtime {} failed; terminating engine lifecycle",
                    failedRuntime.getRevisionId(),
                    failure
            );
            closeRevisionSubscription();
            stopAndCloseActiveRuntime();
            terminated.countDown();
        }
    }

    private void startInitialRuntime(ProjectRuntime runtime, ProjectRevisionHandle revision) {
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
            ProjectRevisionHandle previousRevision,
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
        ProjectRevisionHandle revision = activeRevision;
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

    private void closeRevision(ProjectRevisionHandle revision, Throwable failure) {
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

    private void closeRevisionSubscription() {
        ProjectRevisionSubscription currentSubscription = revisionSubscription;
        if (currentSubscription != null) {
            currentSubscription.close();
        }
    }

    private final class RevisionDrainCommand implements Runnable {
        @Override
        public void run() {
            ProjectRevisionHandle revision;
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
