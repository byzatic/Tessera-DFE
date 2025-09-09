package io.github.byzatic.tessera.engine.infrastructure.persistence.resource_manager;

import io.github.byzatic.tessera.engine.domain.repository.*;

import java.util.List;

public interface ResourceFactoryInterface {
    JpaLikeNodeGlobalRepositoryInterface createNodeGlobalRepository(String projectName);

    JpaLikeNodeRepositoryInterface createNodeRepository(String projectName);

    JpaLikePipelineRepositoryInterface createPipelineRepository(String projectName);

    JpaLikeProjectGlobalRepositoryInterface createProjectGlobalRepository(String projectName);

    SharedResourcesRepositoryInterface createSharedResourcesRepository(String projectName);

    List<ResourcesInterface> listResources(String projectName);
}
