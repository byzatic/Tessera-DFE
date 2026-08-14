package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;

import java.time.Duration;

public interface OrchestrationServiceInterface extends AutoCloseable {
    void start() throws BusinessLogicException;

    boolean awaitStarted(Duration timeout) throws InterruptedException;

    void stop();
}
