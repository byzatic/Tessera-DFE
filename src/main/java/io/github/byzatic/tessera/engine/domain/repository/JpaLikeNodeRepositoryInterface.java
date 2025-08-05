package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;

import java.util.List;

public interface JpaLikeNodeRepositoryInterface {
    void reload() throws OperationIncompleteException;

    NodeItem getNode(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    List<GraphNodeRef> getAllGraphNodeRef() throws OperationIncompleteException;
}
