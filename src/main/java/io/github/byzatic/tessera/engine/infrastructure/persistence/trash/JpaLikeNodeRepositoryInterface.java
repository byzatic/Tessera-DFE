package io.github.byzatic.tessera.engine.infrastructure.persistence.trash;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;

import java.util.List;

public interface JpaLikeNodeRepositoryInterface extends ResourcesInterface {
    void reload();

    NodeItem getNode(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    List<GraphNodeRef> getAllGraphNodeRef() throws OperationIncompleteException;
}
