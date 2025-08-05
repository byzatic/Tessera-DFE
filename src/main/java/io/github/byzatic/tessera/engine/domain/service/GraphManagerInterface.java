package io.github.byzatic.tessera.engine.domain.service;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface GraphManagerInterface {

    void runGraph() throws OperationIncompleteException;
}
