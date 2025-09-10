package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectRepositoryImpl;

public interface ProjectRepository extends GlobalRepository, NodeRepository, ResourcesRepository {
    void addProjectLoader(ProjectRepositoryImpl.ProjectLoaderTypes projectLoaderType, ProjectLoaderInterface projectLoader);
    void load() throws OperationIncompleteException;
    void reload() throws OperationIncompleteException;
}
