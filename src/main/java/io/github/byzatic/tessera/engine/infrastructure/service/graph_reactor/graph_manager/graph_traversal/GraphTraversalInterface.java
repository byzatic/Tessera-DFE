package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import org.jetbrains.annotations.NotNull;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;

public interface GraphTraversalInterface {
    void traverse(@NotNull Node root) throws OperationIncompleteException;
}
