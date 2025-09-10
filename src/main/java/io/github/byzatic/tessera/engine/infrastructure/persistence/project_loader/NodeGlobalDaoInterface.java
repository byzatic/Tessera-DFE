package io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.common.NodeToGNRContainer;

import java.util.Map;

public interface NodeGlobalDaoInterface {
    Map<GraphNodeRef, NodeGlobal> load(String projectName, NodeToGNRContainer nodeToGNRContainer) throws OperationIncompleteException;
}
