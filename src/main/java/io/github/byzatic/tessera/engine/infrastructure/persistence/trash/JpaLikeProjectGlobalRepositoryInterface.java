package io.github.byzatic.tessera.engine.infrastructure.persistence.trash;

import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;

public interface JpaLikeProjectGlobalRepositoryInterface extends ResourcesInterface {
    ProjectGlobal getProjectGlobal();

    Boolean containsProjectGlobalForStorage(String storageId);

    void reload();
}
