package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository;

import org.jetbrains.annotations.NotNull;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;

import java.util.List;

public interface GraphManagerNodeRepositoryInterface {
    @NotNull Node getNode(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    @NotNull List<GraphNodeRef> listGraphNodeRef();

    @NotNull List<Node> getNodeDownstream(Node node) throws OperationIncompleteException;

    @NotNull List<Node> getNodeDownstream(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    @NotNull List<GraphNodeRef> getRootNodes() throws OperationIncompleteException;
    @NotNull void clearNodeStatuses() throws OperationIncompleteException;
}
