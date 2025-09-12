package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto;

import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;

public class GlobalContainer {

    private final ProjectGlobal projectGlobal;

    public GlobalContainer(ProjectGlobal projectGlobal) {
        this.projectGlobal = projectGlobal;
    }

    public ProjectGlobal getProjectGlobal() {
        return projectGlobal;
    }
}
