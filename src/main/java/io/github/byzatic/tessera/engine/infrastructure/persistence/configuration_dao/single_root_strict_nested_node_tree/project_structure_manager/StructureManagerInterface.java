package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_manager;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;

public interface StructureManagerInterface {
    NodeStructure getNodeStructure(GraphNodeRef graphNodeRef);

    ProjectStructure getProjectStructure();
}
