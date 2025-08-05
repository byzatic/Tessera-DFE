package io.github.byzatic.tessera.engine.domain.repository.storage;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface StorageManagerInterface extends GlobalStorageManagerInterface, NodeStorageManagerInterface {
    void cleanupNodeStorages() throws OperationIncompleteException;
}
