package io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager;

import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.*;

import java.util.List;

public interface ResourceFactoryInterface {
    JpaLikeNodeGlobalRepositoryInterface createNodeGlobalRepository(String projectName);

    JpaLikeNodeRepositoryInterface createNodeRepository(String projectName);

    JpaLikePipelineRepositoryInterface createPipelineRepository(String projectName);

    JpaLikeProjectGlobalRepositoryInterface createProjectGlobalRepository(String projectName);

    SharedResourcesRepositoryInterface createSharedResourcesRepository(String projectName);

    StructureControllerInterface createStructureManager(String projectName);

    List<ResourcesInterface> listResources(String projectName);
}
