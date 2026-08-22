package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper;

import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;

public final class ProjectRepositoryStateDataObject {

    private final GlobalContainer globalContainer;
    private final NodeContainer nodeContainer;

    public ProjectRepositoryStateDataObject(
            GlobalContainer globalContainer,
            NodeContainer nodeContainer
    ) {
        this.globalContainer = globalContainer;
        this.nodeContainer = nodeContainer;
    }

    public GlobalContainer getGlobalContainer() {
        return globalContainer;
    }

    public NodeContainer getNodeContainer() {
        return nodeContainer;
    }
}
