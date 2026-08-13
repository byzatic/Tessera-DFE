package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper;

import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;

public final class ProjectRepositoryStateDataObject {

    private final GlobalContainer globalContainer;
    private final NodeContainer nodeContainer;
    private final SharedResourcesContainer sharedResourcesContainer;

    public ProjectRepositoryStateDataObject(
            GlobalContainer globalContainer,
            NodeContainer nodeContainer,
            SharedResourcesContainer sharedResourcesContainer
    ) {
        this.globalContainer = globalContainer;
        this.nodeContainer = nodeContainer;
        this.sharedResourcesContainer = sharedResourcesContainer;
    }

    public GlobalContainer getGlobalContainer() {
        return globalContainer;
    }

    public NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    public SharedResourcesContainer getSharedResourcesContainer() {
        return sharedResourcesContainer;
    }
}
