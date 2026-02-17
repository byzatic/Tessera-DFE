package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface StorageInterface<T extends DataValueInterface> {
    String getStorageId();

    void create(DataLookupIdentifierImpl id, T item) throws OperationIncompleteException;

    @NotNull T read(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    @NotNull Boolean update(DataLookupIdentifierImpl id, T item) throws OperationIncompleteException;

    @NotNull Boolean delete(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    @NotNull List<Map.Entry<String, T>> list() throws OperationIncompleteException;

    @NotNull Boolean contains(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    @NotNull Integer size();

    void cleanup();
}
