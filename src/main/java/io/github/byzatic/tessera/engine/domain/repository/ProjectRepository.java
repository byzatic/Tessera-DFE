package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectRepositoryImpl;

public interface ProjectRepository extends FullProjectRepository, Reloadable {
    public enum ProjectLoaderTypes{PLV1}
    void addProjectLoader(ProjectLoaderTypes projectLoaderType, ProjectLoaderInterface projectLoader);
}
