package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import org.jetbrains.annotations.NotNull;

public interface GraphTraversalInterface {
    void cancel();

    void traverse(@NotNull Node root) throws OperationIncompleteException;
}
