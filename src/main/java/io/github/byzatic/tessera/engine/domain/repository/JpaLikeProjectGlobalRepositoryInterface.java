package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;

public interface JpaLikeProjectGlobalRepositoryInterface extends ResourcesInterface {
    ProjectGlobal getProjectGlobal();

    Boolean containsProjectGlobalForStorage(String storageId);

    void reload();
}
