package io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_manager;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.repository.ResourcesInterface;

public interface StructureControllerInterface extends ResourcesInterface {
    NodeStructure getNodeStructure(GraphNodeRef graphNodeRef);

    ProjectStructure getProjectStructure();
}
