package io.github.byzatic.tessera.engine.infrastructure.persistence.resource_manager.common_v1_hierarchical;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.repository.*;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository.JpaLikeNodeGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository.NodeGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository.JpaLikeNodeRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository.ProjectDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.JpaLikePipelineRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.PipelineDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository.JpaLikeProjectGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository.ProjectGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.resource_manager.ResourceFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.shared_resources_manager.SharedResourcesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceFactoryV1Hierarchical implements ResourceFactoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(ResourceFactoryV1Hierarchical.class);
    private final NodeGlobalDaoInterface nodeGlobalDao;
    private final ProjectDaoInterface projectDao;
    private final PipelineDaoInterface pipelineDao;
    private final ProjectGlobalDaoInterface projectGlobalDao;

    private enum ResourceId {NODE_GLOBAL_REPOSITORY, NODE_REPOSITORY, PIPELINE_REPOSITORY, PROJECT_GLOBAL_REPOSITORY, SHARED_RESOURCES_REPOSITORY}

    private final Map<String, Map<ResourceId, ResourcesInterface>> workflowResourceMap = new ConcurrentHashMap<>();

    public ResourceFactoryV1Hierarchical(
            NodeGlobalDaoInterface nodeGlobalDao,
            ProjectDaoInterface projectDao,
            PipelineDaoInterface pipelineDao,
            ProjectGlobalDaoInterface projectGlobalDao
    ) {
        this.nodeGlobalDao = nodeGlobalDao;
        this.projectDao = projectDao;
        this.pipelineDao = pipelineDao;
        this.projectGlobalDao = projectGlobalDao;
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
        if (workflowResourceMap.containsKey(projectName)) {
            Map<ResourceId, ResourcesInterface> resourceMap = workflowResourceMap.get(projectName);
            if (resourceMap.containsKey(resourceId)) {
                resource = resourceMap.get(resourceId);
            } else {
                String errMessage = "Illegal state of resource map for project <" + projectName + ">; Resource <" + resourceId.name() + "> not found";
                logger.error(errMessage);
                throw new IllegalStateException(errMessage);
            }
        } else {
            Map<ResourceId, ResourcesInterface> resourceMap = new ConcurrentHashMap<>();

            try {
                JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository = new JpaLikeNodeGlobalRepository(
                        projectName,
                        nodeGlobalDao
                );
                resourceMap.put(ResourceId.NODE_GLOBAL_REPOSITORY, nodeGlobalRepository);

                JpaLikeNodeRepositoryInterface nodeRepository = new JpaLikeNodeRepository(
                        projectName,
                        projectDao
                );
                resourceMap.put(ResourceId.NODE_REPOSITORY, nodeRepository);

                JpaLikePipelineRepositoryInterface pipelineRepository = new JpaLikePipelineRepository(
                        projectName,
                        pipelineDao
                );
                resourceMap.put(ResourceId.PIPELINE_REPOSITORY, pipelineRepository);

                JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository = new JpaLikeProjectGlobalRepository(
                        projectName,
                        projectGlobalDao
                );
                resourceMap.put(ResourceId.PROJECT_GLOBAL_REPOSITORY, projectGlobalRepository);

                SharedResourcesRepositoryInterface sharedResourcesRepository = new SharedResourcesRepository(
                        projectName
                );
                resourceMap.put(ResourceId.SHARED_RESOURCES_REPOSITORY, sharedResourcesRepository);
            } catch (OperationIncompleteException e) {
                throw new RuntimeException(e);
            }

            workflowResourceMap.put(projectName, resourceMap);

            resource = getResource(projectName, resourceId);
        }
        return resource;
    }

}
