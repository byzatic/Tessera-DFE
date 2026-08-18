package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.lib.configio.application.revision.ProjectRevision;
import io.github.byzatic.lib.configio.application.revision.ProjectRevisionListener;
import io.github.byzatic.lib.configio.application.revision.ProjectRevisionSource;
import io.github.byzatic.lib.configio.domain.exception.ProjectRevisionException;
import io.github.byzatic.lib.configio.domain.model.NodeContainerDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectGlobalDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectStructureDataObject;
import io.github.byzatic.lib.configio.domain.model.SharedResourcesContainerDataObject;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
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
        StubRevisionSource source = new StubRevisionSource();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                source,
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevision previous = createRevision("previous");
        ProjectRevision candidate = createRevision("candidate");

        try {
            coordinator.start();
            source.publish(previous);
            assertTrue(factory.initialRuntime.awaitStarted());

            source.publish(candidate);
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
        StubRevisionSource source = new StubRevisionSource();
        StubRuntimeFactory factory = new StubRuntimeFactory();
        ProjectReloadCoordinator coordinator = new ProjectReloadCoordinator(
                source,
                factory,
                Duration.ofSeconds(2L),
                Duration.ofSeconds(2L)
        );
        ProjectRevision revision = createRevision("previous");
        RuntimeException expectedFailure = new RuntimeException("Expected runtime failure");

        try {
            coordinator.start();
            source.publish(revision);
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

    private ProjectRevision createRevision(String revisionId) throws Exception {
        Path revisionDirectory = temporaryFolder.newFolder(revisionId).toPath();
        Path projectDirectory = revisionDirectory.resolve("project");
        Files.createDirectories(projectDirectory);

        ProjectDataObject projectData = new ProjectDataObject("v1", revisionId);
        ProjectStructureDataObject projectStructure = new ProjectStructureDataObject(
                projectData,
                new HashMap<>()
        );
        NodeContainerDataObject nodes = new NodeContainerDataObject(
                projectStructure,
                new HashMap<>(),
                new HashMap<>()
        );
        ProjectGlobalDataObject global = new ProjectGlobalDataObject(
                new ArrayList<>(),
                new ArrayList<>()
        );
        SharedResourcesContainerDataObject resources =
                new SharedResourcesContainerDataObject(
                        new ArrayList<ClassLoader>(),
                        new ArrayList<URLClassLoader>()
                );
        ProjectLoadResultDataObject loadedProject = new ProjectLoadResultDataObject(
                projectDirectory,
                global,
                nodes,
                resources
        );
        return new ProjectRevision(
                revisionId,
                revisionDirectory.resolve("source.zip"),
                projectDirectory,
                revisionDirectory,
                loadedProject
        );
    }

    private static final class StubRevisionSource implements ProjectRevisionSource {

        private ProjectRevisionListener listener;

        @Override
        public void start(ProjectRevisionListener value) throws ProjectRevisionException {
            listener = value;
        }

        @Override
        public void close() {
        }

        private void publish(ProjectRevision revision) {
            listener.onRevisionAvailable(revision);
        }
    }

    private static final class StubRuntimeFactory implements ProjectRuntimeFactory {

        private final AtomicInteger previousCreations = new AtomicInteger();
        private final StubRuntime initialRuntime = new StubRuntime("previous", false);
        private final StubRuntime failedRuntime = new StubRuntime("candidate", true);
        private final StubRuntime rollbackRuntime = new StubRuntime("previous", false);

        @Override
        public ProjectRuntime create(ProjectRevision revision) {
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

        private void fail(Throwable failure) {
            failureListener.onFailure(this, failure);
        }
    }
}
