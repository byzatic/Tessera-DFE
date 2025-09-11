package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface NodeRepository {
    // ENTITY
    @NotNull List<GraphNodeRef> listGraphNodeRef() throws OperationIncompleteException;

    // NODE
    @NotNull NodeItem getNode(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    // NODE GLOBAL
    @NotNull NodeGlobal getNodeGlobal(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException;
    @NotNull Boolean isNodeStorageDeclaration(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageName);

    // NODE PIPELINE
    @NotNull NodePipeline getPipeline(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException;

}
