package io.github.byzatic.tessera.engine.domain.service;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface ServicesManagerInterface {
    void runAllServices() throws OperationIncompleteException;

    void stopAllServices() throws OperationIncompleteException;
}
