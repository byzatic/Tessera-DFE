package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;
import org.jetbrains.annotations.NotNull;

public interface ProjectLoaderInterface {
    @NotNull GlobalContainer getGlobalContainer(@NotNull String projectName);

    @NotNull NodeContainer getNodeContainer(@NotNull String projectName);

    @NotNull SharedResourcesContainer getSharedResourcesContainer(@NotNull String projectName);
}
