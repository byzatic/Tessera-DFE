package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.NodeToGNRContainer;
import org.jetbrains.annotations.NotNull;

public interface CommonRepository {
    // ENTITY
    @NotNull NodeToGNRContainer getNodeToGNRContainer() throws OperationIncompleteException;
}
