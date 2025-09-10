package io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager;

import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.*;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;

public class ResourceManager implements ResourceManagerInterface {
    private final ResourceFactoryInterface resourceFactory;

    public ResourceManager(ResourceFactoryInterface resourceFactory) {
        this.resourceFactory = resourceFactory;
    }

    @Override
    public <T extends ResourcesInterface> T getResource(String projectName, Class<T> type) {
        T resource = null;
        if (type.equals(JpaLikeNodeGlobalRepositoryInterface.class)) {
            resource = type.cast(resourceFactory.createNodeGlobalRepository(projectName));
        } else if (type.equals(JpaLikeNodeRepositoryInterface.class)) {
            resource = type.cast(resourceFactory.createNodeRepository(projectName));
        } else if (type.equals(JpaLikePipelineRepositoryInterface.class)) {
            resource = type.cast(resourceFactory.createPipelineRepository(projectName));
        } else if (type.equals(JpaLikeProjectGlobalRepositoryInterface.class)) {
            resource = type.cast(resourceFactory.createProjectGlobalRepository(projectName));
        } else if (type.equals(SharedResourcesRepositoryInterface.class)) {
            resource = type.cast(resourceFactory.createSharedResourcesRepository(projectName));
        } else if (type.equals(StructureControllerInterface.class)) {
            resource = type.cast(resourceFactory.createStructureManager(projectName));
        } else {
            throw new IllegalArgumentException("Unsupported resource type: " + type.getName());
        }
        return resource;
    }

    @Override
    public <T extends ResourcesInterface> void reloadResource(String projectName, Class<T> type) {
        getResource(projectName, type).reload();
    }

    @Override
    public void reloadAll(String projectName) {
        for (ResourcesInterface resource : resourceFactory.listResources(projectName)) {
            resource.reload();
        }
    }

    @Override
    public <T extends ResourcesInterface> void loadResourceProject(String projectName, Class<T> type) {
        getResource(projectName, type).load();
    }

    @Override
    public void loadAll(String projectName) {
        for (ResourcesInterface resource : resourceFactory.listResources(projectName)) {
            resource.load();
        }
    }
}
