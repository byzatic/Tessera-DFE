package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ResourcesRepository {
    // ENTITY
    @NotNull List<GraphNodeRef> listGraphNodeRef() throws OperationIncompleteException;

    // SHARED RESOURCES
    @Nullable ClassLoader getSharedResourcesClassLoader();
}
