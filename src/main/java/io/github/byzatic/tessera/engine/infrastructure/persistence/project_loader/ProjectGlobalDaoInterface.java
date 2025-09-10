package io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;

public interface ProjectGlobalDaoInterface {
    ProjectGlobal load(String projectName) throws OperationIncompleteException;
}
