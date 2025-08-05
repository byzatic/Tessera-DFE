package io.github.byzatic.tessera.engine.graph_reactor.dto.graph_manager;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversal;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.NodeLifecycleState;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepository;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactory;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphTraversalTest {

    private String genRndId() {
        return new java.util.Random().ints(8, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())
                .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    @Test
    public void testSingleNodeProcessing() throws OperationIncompleteException {
        GraphNodeRef nodeGraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        Node node = Node.newBuilder()
                .setGraphNodeRef(nodeGraphNodeRef)
                .setDownstream(new ArrayList<>())
                .build();

        Map<GraphNodeRef, Node> allNodes = new HashMap<>();
        allNodes.put(nodeGraphNodeRef, node);

        PipelineManagerFactoryInterface pipelineManagerFactory = new PipelineManagerFactory(TestPipelineManager.class);

        GraphTraversal traversal = new GraphTraversal(new GraphManagerNodeRepository(allNodes), pipelineManagerFactory);
        traversal.traverse(node);

        assertEquals("Node should be READY after traversal", NodeLifecycleState.READY, node.getNodeLifecycleState());
    }

    @Test
    public void testTreeProcessing() throws InterruptedException, OperationIncompleteException {

        Map<GraphNodeRef, Node> allNodes = new HashMap<>();

        GraphNodeRef child11GraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        Node child11 = Node.newBuilder()
                .setGraphNodeRef(child11GraphNodeRef)
                .setDownstream(new ArrayList<>())
                .build();
        allNodes.put(child11GraphNodeRef, child11);

        GraphNodeRef child1GraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        List<GraphNodeRef> child1GraphNodeRefs = new ArrayList<>();
        child1GraphNodeRefs.add(child11GraphNodeRef);
        Node child1 = Node.newBuilder()
                .setGraphNodeRef(child1GraphNodeRef)
                .setDownstream(child1GraphNodeRefs)
                .build();
        allNodes.put(child1GraphNodeRef, child1);

        GraphNodeRef chil2GraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        Node child2 = Node.newBuilder()
                .setGraphNodeRef(chil2GraphNodeRef)
                .setDownstream(new ArrayList<>())
                .build();
        allNodes.put(chil2GraphNodeRef, child2);

        GraphNodeRef rootGraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        List<GraphNodeRef> rootGraphNodeRefs = new ArrayList<>();
        rootGraphNodeRefs.add(child1GraphNodeRef);
        rootGraphNodeRefs.add(chil2GraphNodeRef);
        Node root = Node.newBuilder()
                .setGraphNodeRef(rootGraphNodeRef)
                .setDownstream(rootGraphNodeRefs)
                .build();
        allNodes.put(rootGraphNodeRef, root);

        PipelineManagerFactoryInterface pipelineManagerFactory = new PipelineManagerFactory(TestPipelineManager.class);

        GraphTraversal traversal = new GraphTraversal(new GraphManagerNodeRepository(allNodes), pipelineManagerFactory);
        traversal.traverse(root);

        assertEquals("Root should be READY", NodeLifecycleState.READY, root.getNodeLifecycleState());
        assertEquals("Child1 should be READY", NodeLifecycleState.READY, child1.getNodeLifecycleState());
        assertEquals("Child2 should be READY", NodeLifecycleState.READY, child2.getNodeLifecycleState());
        assertEquals("Child11 should be READY", NodeLifecycleState.READY, child11.getNodeLifecycleState());
    }

    @Test
    public void testWaitingForChild() throws InterruptedException, ExecutionException, OperationIncompleteException {

        Map<GraphNodeRef, Node> allNodes = new HashMap<>();

        GraphNodeRef slowChildGraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        Node slowChild = Node.newBuilder()
                .setGraphNodeRef(slowChildGraphNodeRef)
                .setDownstream(new ArrayList<>())
                .build();
        allNodes.put(slowChildGraphNodeRef, slowChild);

        GraphNodeRef rootGraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        List<GraphNodeRef> rootGraphNodeRefs = new ArrayList<>();
        rootGraphNodeRefs.add(slowChildGraphNodeRef);
        Node root = Node.newBuilder()
                .setGraphNodeRef(rootGraphNodeRef)
                .setDownstream(rootGraphNodeRefs)
                .build();
        allNodes.put(rootGraphNodeRef, root);


        // Параллельно обрабатываем slowChild с задержкой
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                Thread.sleep(300); // эмулируем долгую работу
                slowChild.setNodeLifecycleState(NodeLifecycleState.READY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        PipelineManagerFactoryInterface pipelineManagerFactory = new PipelineManagerFactory(TestWithWaitingPipelineManager.class);

        GraphTraversal traversal = new GraphTraversal(new GraphManagerNodeRepository(allNodes), pipelineManagerFactory);
        long start = System.currentTimeMillis();
        traversal.traverse(root);
        long duration = System.currentTimeMillis() - start;

        future.get(); // Убедимся что поток точно завершился
        executor.shutdown();

        assertEquals("Slow child should be READY after traversal", NodeLifecycleState.READY, slowChild.getNodeLifecycleState());
        assertEquals("Root should be READY after traversal", NodeLifecycleState.READY, root.getNodeLifecycleState());

        assertTrue("Traversal should wait for slow child to be processed", duration >= 300);
    }


    public static class TestWithWaitingPipelineManager implements PipelineManagerInterface {
        private final static Logger logger= LoggerFactory.getLogger(TestWithWaitingPipelineManager.class);
        public TestWithWaitingPipelineManager() {
        }
        @Override
        public void runPipeline() throws OperationIncompleteException {
            try {
                logger.debug("Test run (sleep 310L)");
                Thread.sleep(310L);
                logger.debug("Test run (sleep 310L) complete");
            } catch (Exception e) {
                throw new OperationIncompleteException(e);
            }
        }
    }

    public static class TestPipelineManager implements PipelineManagerInterface {
        private final static Logger logger= LoggerFactory.getLogger(TestPipelineManager.class);
        public TestPipelineManager() {
        }
        @Override
        public void runPipeline() throws OperationIncompleteException {
            logger.debug("Test run complete");
        }
    }
}