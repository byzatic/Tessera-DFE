package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;

public interface JpaLikeProjectGlobalRepositoryInterface {
    ProjectGlobal getProjectGlobal();

    Boolean isStorageWithId(String storageId);

    void reload() throws OperationIncompleteException;
}
