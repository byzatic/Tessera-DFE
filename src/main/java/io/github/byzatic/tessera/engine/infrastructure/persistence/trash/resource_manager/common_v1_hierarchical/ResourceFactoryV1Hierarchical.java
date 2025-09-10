package io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.common_v1_hierarchical;

import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.*;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.ResourceFactoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceFactoryV1Hierarchical implements ResourceFactoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(ResourceFactoryV1Hierarchical.class);

    private enum ResourceId {NODE_GLOBAL_REPOSITORY, NODE_REPOSITORY, PIPELINE_REPOSITORY, PROJECT_GLOBAL_REPOSITORY, SHARED_RESOURCES_REPOSITORY, STRUCTURE_MANAGER}

    private final Map<String, ResourcesContext> workflowResourceMap = new ConcurrentHashMap<>();

    public ResourceFactoryV1Hierarchical() {
    }

    @Override
    public JpaLikeNodeGlobalRepositoryInterface createNodeGlobalRepository(String projectName) {
        return (JpaLikeNodeGlobalRepositoryInterface) getResource(projectName, ResourceId.NODE_GLOBAL_REPOSITORY);
    }

    @Override
    public JpaLikeNodeRepositoryInterface createNodeRepository(String projectName) {
        return (JpaLikeNodeRepositoryInterface) getResource(projectName, ResourceId.NODE_REPOSITORY);
    }

    @Override
    public JpaLikePipelineRepositoryInterface createPipelineRepository(String projectName) {
        return (JpaLikePipelineRepositoryInterface) getResource(projectName, ResourceId.PIPELINE_REPOSITORY);
    }

    @Override
    public JpaLikeProjectGlobalRepositoryInterface createProjectGlobalRepository(String projectName) {
        return (JpaLikeProjectGlobalRepositoryInterface) getResource(projectName, ResourceId.PROJECT_GLOBAL_REPOSITORY);
    }

    @Override
    public SharedResourcesRepositoryInterface createSharedResourcesRepository(String projectName) {
        return (SharedResourcesRepositoryInterface) getResource(projectName, ResourceId.SHARED_RESOURCES_REPOSITORY);
    }

    @Override
    public StructureControllerInterface createStructureManager(String projectName) {
        return (StructureControllerInterface) getResource(projectName, ResourceId.STRUCTURE_MANAGER);
    }

    @Override
    public List<ResourcesInterface> listResources(String projectName) {
        List<ResourcesInterface> resourcesList = new ArrayList<>();
        resourcesList.add(createNodeGlobalRepository(projectName));
        resourcesList.add(createNodeRepository(projectName));
        resourcesList.add(createPipelineRepository(projectName));
        resourcesList.add(createProjectGlobalRepository(projectName));
        resourcesList.add(createSharedResourcesRepository(projectName));
        return resourcesList;
    }

    private ResourcesInterface getResource(String projectName, ResourceId resourceId) {
        ResourcesInterface resource = null;

        if (!isResourceExists(projectName, resourceId)) {
            logger.debug("Resource not exists: {} - {}", projectName, resourceId);
            createResource(projectName);
        }

        switch (resourceId) {
            case SHARED_RESOURCES_REPOSITORY -> {
                resource = workflowResourceMap.get(projectName).getSharedResourcesManager();
            }
            case NODE_GLOBAL_REPOSITORY -> {
                resource = workflowResourceMap.get(projectName).getNodeGlobalRepository();
            }
            case NODE_REPOSITORY -> {
                resource = workflowResourceMap.get(projectName).getNodeRepository();
            }
            case PIPELINE_REPOSITORY -> {
                resource = workflowResourceMap.get(projectName).getPipelineRepository();
            }
            case PROJECT_GLOBAL_REPOSITORY -> {
                resource = workflowResourceMap.get(projectName).getProjectGlobalRepository();
            }
            case STRUCTURE_MANAGER -> {
                resource = workflowResourceMap.get(projectName).getStructureManager();
            }
            default -> {

            }
        }

        return resource;
    }

    private void createResource(String projectName) {
        workflowResourceMap.put(projectName, new ResourcesContext(projectName));
    }

    private boolean isResourceExists(String projectName, ResourceId resourceId) {
        return workflowResourceMap.containsKey(projectName);
    }

}
