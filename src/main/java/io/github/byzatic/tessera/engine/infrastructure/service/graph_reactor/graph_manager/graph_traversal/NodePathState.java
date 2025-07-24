package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;

import java.util.List;

class NodePathState {
    Node node;
    List<Node> pathSoFar;

    NodePathState(Node node, List<Node> pathSoFar) {
        this.node = node;
        this.pathSoFar = pathSoFar;
    }
}
