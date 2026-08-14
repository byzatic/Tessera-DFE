package io.github.byzatic.tessera.engine.domain.repository.storage;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface StorageManagerInterface extends GlobalStorageManagerInterface, NodeStorageManagerInterface {
    void cleanupNodeStorages() throws OperationIncompleteException;

    /**
     * Releases all values retained by project-scoped storages.
     *
     * @throws OperationIncompleteException when a storage cannot be cleaned
     */
    void cleanupStorages() throws OperationIncompleteException;
}
