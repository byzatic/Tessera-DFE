package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;

public interface OrchestrationServiceInterface extends AutoCloseable {
    void start() throws BusinessLogicException;
}
