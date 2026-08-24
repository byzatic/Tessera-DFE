package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.observability.PrometheusMetricsAgent;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerInterface;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GraphManagerFactoryTest {

    private static final String TEST_CONFIGURATION =
            "<Configuration><projectName>graph-manager-test</projectName></Configuration>";

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private static String originalConfigurationFilePath;
    private static String originalDataDirectory;

    @BeforeClass
    public static void startMetricsAgent() throws Exception {
        originalConfigurationFilePath = System.getProperty("configFilePath");
        originalDataDirectory = System.getProperty("dataDirectory");
        Path configurationFile = TEMPORARY_FOLDER.newFile("configuration.xml").toPath();
        Path dataDirectory = TEMPORARY_FOLDER.newFolder("data").toPath();
        Files.writeString(configurationFile, TEST_CONFIGURATION, StandardCharsets.UTF_8);
        System.setProperty("configFilePath", configurationFile.toString());
        System.setProperty("dataDirectory", dataDirectory.toString());
        PrometheusMetricsAgent.getInstance().start(new URI("http://127.0.0.1:0"));
    }

    @AfterClass
    public static void stopMetricsAgent() {
        PrometheusMetricsAgent.getInstance().stop();
        if (originalConfigurationFilePath == null) {
            System.clearProperty("configFilePath");
        } else {
            System.setProperty("configFilePath", originalConfigurationFilePath);
        }
        if (originalDataDirectory == null) {
            System.clearProperty("dataDirectory");
        } else {
            System.setProperty("dataDirectory", originalDataDirectory);
        }
    }

    @Test
    public void shouldCleanupOnceWhenSelfHostedGraphIsEmpty() throws Exception {
        StorageManagerInterface storageManager = mock(StorageManagerInterface.class);
        GraphManagerNodeRepositoryInterface nodeRepository = mock(GraphManagerNodeRepositoryInterface.class);
        PipelineManagerFactoryInterface pipelineManagerFactory = mock(PipelineManagerFactoryInterface.class);
        when(nodeRepository.getRootNodes()).thenReturn(Collections.emptyList());
        GraphManagerInterface graphManager = new GraphManagerFactory(
                storageManager,
                nodeRepository,
                pipelineManagerFactory
        ).create();

        graphManager.runGraph();

        verify(storageManager, times(1)).cleanupNodeStorages();
        verify(nodeRepository, times(1)).clearNodeStatuses();
    }

    @Test
    public void shouldRunAndCleanupOnceWhenSelfHostedGraphIsNotEmpty() throws Exception {
        StorageManagerInterface storageManager = mock(StorageManagerInterface.class);
        GraphManagerNodeRepositoryInterface nodeRepository = mock(GraphManagerNodeRepositoryInterface.class);
        PipelineManagerFactoryInterface pipelineManagerFactory = mock(PipelineManagerFactoryInterface.class);
        PipelineManagerInterface pipelineManager = mock(PipelineManagerInterface.class);
        GraphNodeRef rootRef = GraphNodeRef.newBuilder().nodeUUID("root").build();
        Node root = Node.newBuilder()
                .setGraphNodeRef(rootRef)
                .setDownstream(Collections.emptyList())
                .build();
        when(nodeRepository.getRootNodes()).thenReturn(Collections.singletonList(rootRef));
        when(nodeRepository.getNode(rootRef)).thenReturn(root);
        when(nodeRepository.getNodeDownstream(root)).thenReturn(Collections.emptyList());
        when(pipelineManagerFactory.getNewPipelineManager(eq(rootRef), anyList()))
                .thenReturn(pipelineManager);
        GraphManagerInterface graphManager = new GraphManagerFactory(
                storageManager,
                nodeRepository,
                pipelineManagerFactory
        ).create();

        graphManager.runGraph();

        verify(pipelineManager, times(1)).runPipeline();
        verify(storageManager, times(1)).cleanupNodeStorages();
        verify(nodeRepository, times(1)).clearNodeStatuses();
    }

    @Test
    public void shouldPreserveExecutionFailureWhenCleanupAlsoFails() throws Exception {
        StorageManagerInterface storageManager = mock(StorageManagerInterface.class);
        GraphManagerNodeRepositoryInterface nodeRepository = mock(GraphManagerNodeRepositoryInterface.class);
        PipelineManagerFactoryInterface pipelineManagerFactory = mock(PipelineManagerFactoryInterface.class);
        OperationIncompleteException executionFailure = new OperationIncompleteException("execution failed");
        OperationIncompleteException cleanupFailure = new OperationIncompleteException("cleanup failed");
        when(nodeRepository.getRootNodes()).thenThrow(executionFailure);
        doThrow(cleanupFailure).when(storageManager).cleanupNodeStorages();
        GraphManagerInterface graphManager = new GraphManagerFactory(
                storageManager,
                nodeRepository,
                pipelineManagerFactory
        ).create();

        try {
            graphManager.runGraph();
            fail("OperationIncompleteException expected");
        } catch (OperationIncompleteException actual) {
            assertSame(executionFailure, actual);
            assertEquals(1, actual.getSuppressed().length);
            assertSame(cleanupFailure, actual.getSuppressed()[0]);
        }

        verify(storageManager, times(1)).cleanupNodeStorages();
    }
}
