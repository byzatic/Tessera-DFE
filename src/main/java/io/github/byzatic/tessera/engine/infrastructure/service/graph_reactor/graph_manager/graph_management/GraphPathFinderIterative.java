package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeNodeRepositoryInterface;

import java.util.*;

class GraphPathFinderIterative {

    private final JpaLikeNodeRepositoryInterface nodeRepository;

    public GraphPathFinderIterative(JpaLikeNodeRepositoryInterface nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public List<List<NodeItem>> findAllPathsTo(GraphNodeRef targetGraphNodeRef) throws OperationIncompleteException {
        try {

            String targetUUID = this.nodeRepository.getNode(targetGraphNodeRef).getUUID();

            List<List<NodeItem>> resultPaths = new ArrayList<>();

            List<GraphNodeRef> allNodes = nodeRepository.getAllGraphNodeRef();
            Set<String> nonSources = new HashSet<>();

            for (GraphNodeRef graphNodeRef : allNodes) {
                for (GraphNodeRef ref : nodeRepository.getNode(graphNodeRef).getDownstream()) {
                    nonSources.add(ref.getNodeUUID());
                }
            }

            List<GraphNodeRef> sources = allNodes.stream()
                    .filter(node -> !nonSources.contains(node.getNodeUUID()))
                    .toList();

            Deque<PathState> stack = new ArrayDeque<>();

            for (GraphNodeRef source : sources) {
                stack.push(new PathState(nodeRepository.getNode(source), new ArrayList<>()));
            }

            while (!stack.isEmpty()) {
                PathState currentState = stack.pop();
                NodeItem currentNode = currentState.node;
                List<NodeItem> currentPath = new ArrayList<>(currentState.pathSoFar);
                currentPath.add(currentNode);

                if (currentNode.getUUID().equals(targetUUID)) {
                    resultPaths.add(currentPath);
                    continue;
                }

                for (GraphNodeRef ref : currentNode.getDownstream()) {
                    NodeItem next = nodeRepository.getNode(ref);
                    if (next != null) {
                        stack.push(new PathState(next, currentPath));
                    }
                }
            }

            return resultPaths;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private static class PathState {
        NodeItem node;
        List<NodeItem> pathSoFar;

        PathState(NodeItem node, List<NodeItem> pathSoFar) {
            this.node = node;
            this.pathSoFar = pathSoFar;
        }
    }
}
