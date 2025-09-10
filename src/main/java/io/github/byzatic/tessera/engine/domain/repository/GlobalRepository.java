package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GlobalRepository {
    // ENTITY
    @NotNull List<GraphNodeRef> listGraphNodeRef() throws OperationIncompleteException;

    // GLOBAL
    @NotNull ProjectGlobal getGlobal();
    @NotNull Boolean isGlobalStorageDeclaration(@NotNull String storageId);
}
