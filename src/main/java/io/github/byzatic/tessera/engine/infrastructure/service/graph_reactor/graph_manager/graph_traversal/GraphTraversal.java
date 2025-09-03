package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;

import java.util.*;
import java.util.stream.Collectors;

public class GraphTraversal implements GraphTraversalInterface{
    private final static Logger logger= LoggerFactory.getLogger(GraphTraversal.class);
    private ImmediateSchedulerInterface immediateScheduler = null;
    private JobEventListener[] listeners = null;
    private PipelineManagerFactoryInterface pipelineManagerFactory = null;
    private GraphManagerNodeRepositoryInterface graphManagerNodeRepository = null;

    public GraphTraversal(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository, PipelineManagerFactoryInterface pipelineManagerFactory) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository, new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory, new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));
        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;
    }

    public GraphTraversal(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository, PipelineManagerFactoryInterface pipelineManagerFactory, ImmediateSchedulerInterface immediateScheduler, JobEventListener... listeners) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository, new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory, new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));
        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;
        this.immediateScheduler = immediateScheduler;
        this.listeners = listeners;
    }

    @Override
    public void traverse(@NotNull Node root) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(root, new IllegalArgumentException(Node.class.getSimpleName()+" should be NotNull"));
            Deque<NodePathState> stack = new ArrayDeque<>();
            stack.push(new NodePathState(root, new ArrayList<>()));
            logger.debug("root jpa_like_node_repository pushed to stack");

            while (!stack.isEmpty()) {
                NodePathState state = stack.peek();
                Node current = state.node;
                List<Node> currentPath = state.pathSoFar;

                switch (current.getNodeLifecycleState()) {
                    case NOTSTATED:
                        current.setNodeLifecycleState(NodeLifecycleState.WAITING);
                        List<Node> downstream = getDownstreamNodes(current);
                        for (Node child : downstream) {
                            List<Node> newPath = new ArrayList<>(currentPath);
                            newPath.add(current);
                            stack.push(new NodePathState(child, newPath));
                        }
                        break;

                    case WAITING:
                        boolean allChildrenReady = true;
                        for (Node child : getDownstreamNodes(current)) {
                            if (child.getNodeLifecycleState() != NodeLifecycleState.READY) {
                                allChildrenReady = false;
                                child.waitUntilReady();
                            }
                        }

                        if (allChildrenReady) {
                            stack.pop();
                            List<Node> fullPath = new ArrayList<>(currentPath);
                            fullPath.add(current);
                            processWithPath(current, fullPath);
                            current.setNodeLifecycleState(NodeLifecycleState.READY);
                        }
                        break;

                    case READY:
                        stack.pop();
                        break;
                }
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private List<Node> getDownstreamNodes(Node current) throws OperationIncompleteException {
        return graphManagerNodeRepository.getNodeDownstream(current);
    }

    private void processWithPath(Node current, List<Node> path) throws OperationIncompleteException {
        try {
            logger.debug("Processing jpa_like_node_repository: {}, path: {}", current, path.stream().map(Node::toString).collect(Collectors.joining(" -> ")));
            if (immediateScheduler != null) {
                pipelineManagerFactory.getNewPipelineManager(current.getGraphNodeRef(), convertPathToRefs(path), immediateScheduler, listeners).runPipeline();
            } else {
                pipelineManagerFactory.getNewPipelineManager(current.getGraphNodeRef(), convertPathToRefs(path)).runPipeline();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new OperationIncompleteException(e);
        }
    }

    private List<GraphNodeRef> convertPathToRefs(List<Node> path) {
        return path.stream()
                .map(Node::getGraphNodeRef)
                .collect(Collectors.toList());
    }
}
