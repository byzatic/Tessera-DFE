package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;

import java.nio.file.Path;

public interface PathManagerInterface {
    Path getStoragePathByGraphNodeRef(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    Path getProjectGlobalStorage();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();
}
