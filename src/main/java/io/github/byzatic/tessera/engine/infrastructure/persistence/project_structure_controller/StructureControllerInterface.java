package io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_controller;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.NodeToGNRContainer;

public interface StructureControllerInterface {
    NodeStructure getNodeStructure(GraphNodeRef graphNodeRef, NodeToGNRContainer nodeToGNRContainer);

    ProjectStructure getProjectStructure();
}
