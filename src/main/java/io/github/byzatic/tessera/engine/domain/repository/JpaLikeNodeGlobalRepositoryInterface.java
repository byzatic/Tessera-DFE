package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;

public interface JpaLikeNodeGlobalRepositoryInterface extends ResourcesInterface {
    NodeGlobal getNodeGlobal(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    Boolean isStorageWithId(GraphNodeRef graphNodeRef, String storageName);

    void reload();
}
