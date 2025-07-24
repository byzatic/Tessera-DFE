package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;

import java.util.List;

public interface GraphPathManagerInterface {
    List<List<NodeItem>> getRootPaths(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    List<String> getRootPathsAsString(GraphNodeRef graphNodeRef, String delimiter) throws OperationIncompleteException;
}
