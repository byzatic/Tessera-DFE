package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphPathManager implements GraphPathManagerInterface {

    private final GraphPathFinderIterative graphPathFinderIterative;

    public GraphPathManager(FullProjectRepository fullProjectRepository) {
        this.graphPathFinderIterative = new GraphPathFinderIterative(fullProjectRepository);
    }

    @Override
    public List<List<NodeItem>> getRootPaths(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        return graphPathFinderIterative.findAllPathsTo(graphNodeRef);
    }

    @Override
    public List<String> getRootPathsAsString(GraphNodeRef graphNodeRef, String delimiter) throws OperationIncompleteException {
        List<List<NodeItem>> paths = graphPathFinderIterative.findAllPathsTo(graphNodeRef);

        List<String> result = new ArrayList<>();
        for (List<NodeItem> path : paths) {
            result.add(path.stream().map(NodeItem::getId).collect(Collectors.joining(delimiter)));
        }

        return result;
    }
}
