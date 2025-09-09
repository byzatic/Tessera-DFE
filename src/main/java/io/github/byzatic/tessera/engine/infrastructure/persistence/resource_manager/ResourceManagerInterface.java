package io.github.byzatic.tessera.engine.infrastructure.persistence.resource_manager;

import io.github.byzatic.tessera.engine.domain.repository.ResourcesInterface;

public interface ResourceManagerInterface {
    <T extends ResourcesInterface> T getResource(String projectName, Class<T> type);

    <T extends ResourcesInterface> void reloadResource(String projectName, Class<T> type);

    void reloadAll(String projectName);

    <T extends ResourcesInterface> void loadResourceProject(String projectName, Class<T> type);

    void loadAll(String projectName);
}
