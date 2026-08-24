package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionError;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionHandle;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionListener;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionSubscription;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRevisionWatchRequest;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.tessera.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.tessera.lib.configio.unified.TesseraProjectIO;
import io.github.byzatic.tessera.lib.configio.unified.model.ExportProjectRequest;
import io.github.byzatic.tessera.lib.configio.unified.model.ProjectConfiguration;
import io.github.byzatic.tessera.lib.configio.unified.model.SaveProjectRequest;
import io.github.byzatic.tessera.lib.configio.unified.model.SaveProjectResult;
import io.github.byzatic.tessera.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProjectReloadCoordinatorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void restoresPreviousRevisionWhenCandidateStartupFails() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle previous = createRevision("previous");
        ProjectRevisionHandle candidate = createRevision("candidate");

        try {
            coordinator.start();
            projectIO.publish(previous);
            assertTrue(factory.initialRuntime.awaitStarted());

            projectIO.publish(candidate);
            assertTrue(factory.rollbackRuntime.awaitStarted());

            assertTrue(factory.initialRuntime.getStopCount() == 1);
            assertTrue(factory.failedRuntime.getStopCount() == 1);
            assertTrue(candidate.isClosed());
            assertFalse(previous.isClosed());
        } finally {
            coordinator.close();
        }

        assertTrue(previous.isClosed());
        assertTrue(factory.rollbackRuntime.getStopCount() == 1);
    }

    @Test
    public void terminatesWhenActiveRuntimeReportsFatalFailure() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle revision = createRevision("previous");
        RuntimeException expectedFailure = new RuntimeException("Expected runtime failure");

        try {
            coordinator.start();
            projectIO.publish(revision);
            assertTrue(factory.initialRuntime.awaitStarted());

            factory.initialRuntime.fail(expectedFailure);

            try {
                coordinator.awaitTermination();
                fail("Coordinator must propagate the active runtime failure");
            } catch (OperationIncompleteException exception) {
                assertSame(expectedFailure, exception.getCause());
            }
            assertTrue(factory.initialRuntime.getStopCount() == 1);
            assertTrue(revision.isClosed());
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void terminatesWhenRollbackRuntimeReportsFatalFailure() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle previous = createRevision("previous");
        ProjectRevisionHandle candidate = createRevision("candidate");
        RuntimeException expectedFailure = new RuntimeException("Expected rollback runtime failure");

        try {
            coordinator.start();
            projectIO.publish(previous);
            assertTrue(factory.initialRuntime.awaitStarted());

            projectIO.publish(candidate);
            assertTrue(factory.rollbackRuntime.awaitStarted());
            assertTrue(factory.rollbackRuntime.wasFailureListenerRegisteredAtStart());

            factory.rollbackRuntime.fail(expectedFailure);

            try {
                coordinator.awaitTermination();
                fail("Coordinator must propagate the rollback runtime failure");
            } catch (OperationIncompleteException exception) {
                assertSame(expectedFailure, exception.getCause());
            }
            assertTrue(factory.rollbackRuntime.getStopCount() == 1);
            assertTrue(previous.isClosed());
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void terminatesWhenInitialRevisionIsRejected() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        RuntimeException expectedFailure = new RuntimeException("Invalid initial revision");

        try {
            coordinator.start();
            projectIO.reject(expectedFailure);

            try {
                coordinator.awaitTermination();
                fail("Coordinator must propagate rejection of the initial revision");
            } catch (OperationIncompleteException exception) {
                assertSame(expectedFailure, exception.getCause());
            }
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void terminatesWhenInitialRevisionDoesNotAppearInTime() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofMillis(50L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );

        try {
            coordinator.start();

            try {
                coordinator.awaitTermination();
                fail("Coordinator must time out while waiting for the initial revision");
            } catch (OperationIncompleteException exception) {
                assertTrue(exception.getCause() instanceof OperationIncompleteException);
                assertTrue(exception.getCause().getMessage().contains(
                        "No project revision reached RUNNING state"
                ));
            }
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void terminatesWhenInitialRuntimeFailsToStart() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory(true, false);
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle revision = createRevision("previous");

        try {
            coordinator.start();
            projectIO.publish(revision);

            try {
                coordinator.awaitTermination();
                fail("Coordinator must propagate initial runtime startup failure");
            } catch (OperationIncompleteException exception) {
                assertTrue(exception.getCause().getMessage().contains("Expected startup failure"));
            }
            assertTrue(revision.isClosed());
            assertTrue(factory.initialRuntime.getStopCount() == 1);
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void terminatesWhenRollbackCannotBeStarted() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory(false, true);
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle previous = createRevision("previous");
        ProjectRevisionHandle candidate = createRevision("candidate");

        try {
            coordinator.start();
            projectIO.publish(previous);
            assertTrue(factory.initialRuntime.awaitStarted());
            projectIO.publish(candidate);

            try {
                coordinator.awaitTermination();
                fail("Coordinator must terminate when rollback cannot be started");
            } catch (OperationIncompleteException exception) {
                assertTrue(exception.getCause().getSuppressed().length == 1);
            }
            assertTrue(previous.isClosed());
            assertTrue(candidate.isClosed());
            assertTrue(factory.rollbackRuntime.getStopCount() == 1);
        } finally {
            coordinator.close();
        }
    }

    @Test
    public void keepsRunningWhenLaterRevisionIsRejected() throws Exception {
        StubProjectIO projectIO = new StubProjectIO();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                projectIO,
                createWatchRequest(),
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevisionHandle previous = createRevision("previous");
        ProjectRevisionHandle candidate = createRevision("candidate");

        try {
            coordinator.start();
            projectIO.publish(previous);
            assertTrue(factory.initialRuntime.awaitStarted());

            projectIO.reject(new RuntimeException("Invalid later revision"));
            projectIO.publish(candidate);

            assertTrue(factory.rollbackRuntime.awaitStarted());
            assertFalse(previous.isClosed());
        } finally {
            coordinator.close();
        }
    }

    private ProjectRevisionWatchRequest createWatchRequest() throws IOException {
        Path sourceArchive = temporaryFolder.newFile("source.zip").toPath();
        Path stagingDirectory = temporaryFolder.newFolder("staging").toPath();
        return ProjectRevisionWatchRequest.builder(sourceArchive, stagingDirectory).build();
    }

    private ProjectRevisionHandle createRevision(String revisionId) throws Exception {
        Path revisionDirectory = temporaryFolder.newFolder(revisionId).toPath();
        Path projectDirectory = revisionDirectory.resolve("project");
        Files.createDirectories(projectDirectory);
        TesseraProject project = TesseraProject.newBuilder()
                .formatVersion("v1")
                .name(revisionId)
                .configuration(ProjectConfiguration.newBuilder().build())
                .nodes(Map.of())
                .build();
        return new StubProjectRevision(
                revisionId,
                revisionDirectory.resolve("source.zip"),
                projectDirectory,
                project
        );
    }

    private static final class StubProjectIO implements TesseraProjectIO {

        private ProjectRevisionListener listener;

        @Override
        public TesseraProject loadProject(Path projectDirectory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveProjectResult saveProject(SaveProjectRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path exportProject(ExportProjectRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProjectRuntimeSession openRuntime(Path projectDirectory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProjectRevisionSubscription watchRevisions(
                ProjectRevisionWatchRequest request,
                ProjectRevisionListener value
        ) {
            listener = value;
            return new StubRevisionSubscription();
        }

        private void publish(ProjectRevisionHandle revision) {
            listener.onRevisionAvailable(revision);
        }

        private void reject(Throwable cause) {
            listener.onRevisionRejected(ProjectRevisionError.newBuilder()
                    .sourceArchive(Path.of("source.zip"))
                    .revisionId("rejected")
                    .cause(cause)
                    .build());
        }
    }

    private static final class StubRevisionSubscription
            implements ProjectRevisionSubscription {

        private boolean closed;

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class StubProjectRevision implements ProjectRevisionHandle {

        private final String revisionId;
        private final Path sourceArchive;
        private final Path projectDirectory;
        private final TesseraProject project;
        private boolean closed;

        private StubProjectRevision(
                String revisionId,
                Path sourceArchive,
                Path projectDirectory,
                TesseraProject project
        ) {
            this.revisionId = revisionId;
            this.sourceArchive = sourceArchive;
            this.projectDirectory = projectDirectory;
            this.project = project;
        }

        @Override
        public String getRevisionId() {
            return revisionId;
        }

        @Override
        public Path getSourceArchive() {
            return sourceArchive;
        }

        @Override
        public Path getProjectDirectory() {
            return projectDirectory;
        }

        @Override
        public TesseraProject getProject() {
            return project;
        }

        @Override
        public ProjectRuntimeSession openRuntime() throws TesseraProjectException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class StubRuntimeFactory implements ProjectRuntimeFactory {

        private final AtomicInteger previousCreations = new AtomicInteger();
        private final StubRuntime initialRuntime;
        private final StubRuntime failedRuntime = new StubRuntime("candidate", true);
        private final StubRuntime rollbackRuntime;

        private StubRuntimeFactory() {
            this(false, false);
        }

        private StubRuntimeFactory(boolean failInitialStart, boolean failRollbackStart) {
            initialRuntime = new StubRuntime("previous", failInitialStart);
            rollbackRuntime = new StubRuntime("previous", failRollbackStart);
        }

        @Override
        public ProjectRuntime create(ProjectRevisionHandle revision) {
            if ("candidate".equals(revision.getRevisionId())) {
                return failedRuntime;
            }
            if (previousCreations.getAndIncrement() == 0) {
                return initialRuntime;
            }
            return rollbackRuntime;
        }
    }

    private static final class StubRuntime implements ProjectRuntime {

        private final String revisionId;
        private final boolean failOnStart;
        private final CountDownLatch started = new CountDownLatch(1);
        private final AtomicInteger stopCount = new AtomicInteger();
        private ProjectRuntimeFailureListener failureListener;
        private boolean failureListenerRegisteredAtStart;

        private StubRuntime(String revisionId, boolean failOnStart) {
            this.revisionId = revisionId;
            this.failOnStart = failOnStart;
        }

        @Override
        public void setFailureListener(ProjectRuntimeFailureListener listener) {
            failureListener = listener;
        }

        @Override
        public void start(Duration startupTimeout) throws OperationIncompleteException {
            failureListenerRegisteredAtStart = failureListener != null;
            if (failOnStart) {
                throw new OperationIncompleteException("Expected startup failure");
            }
            started.countDown();
        }

        @Override
        public void stop(Duration shutdownTimeout) {
            stopCount.incrementAndGet();
        }

        @Override
        public String getRevisionId() {
            return revisionId;
        }

        @Override
        public void close() {
        }

        private boolean awaitStarted() throws InterruptedException {
            return started.await(5L, TimeUnit.SECONDS);
        }

        private int getStopCount() {
            return stopCount.get();
        }

        private boolean wasFailureListenerRegisteredAtStart() {
            return failureListenerRegisteredAtStart;
        }

        private void fail(Throwable failure) {
            failureListener.onFailure(this, failure);
        }
    }
}
