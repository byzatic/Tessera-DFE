package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;

import java.util.Map;

public interface NodeGlobalDaoInterface {
    Map<GraphNodeRef, NodeGlobal> load(String projectName) throws OperationIncompleteException;
}
