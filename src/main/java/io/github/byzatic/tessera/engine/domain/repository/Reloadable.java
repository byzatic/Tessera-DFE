package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface Reloadable {
    void load() throws OperationIncompleteException;
    void reload() throws OperationIncompleteException;
}
