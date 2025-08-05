package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.node.Project;

import java.nio.file.Path;

public interface ProjectDaoInterface {
    Project load(Path projectFile) throws OperationIncompleteException;
}
