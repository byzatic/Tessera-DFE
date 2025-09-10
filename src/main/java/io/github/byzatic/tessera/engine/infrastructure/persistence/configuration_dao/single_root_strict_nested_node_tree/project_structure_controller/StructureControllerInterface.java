package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.common.NodeToGNRContainer;

public interface StructureControllerInterface {
    NodeStructure getNodeStructure(GraphNodeRef graphNodeRef, NodeToGNRContainer nodeToGNRContainer);

    ProjectStructure getProjectStructure();
}
