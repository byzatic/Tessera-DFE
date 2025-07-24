package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import ru.byzatic.metrics_core.mcg3_storageapi_lib.dto.DataValueInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;

import java.util.List;

public interface StorageInterface<T extends DataValueInterface> {
    String getStorageId();

    void create(DataLookupIdentifierImpl id, T item) throws OperationIncompleteException;

    T read(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    Boolean update(DataLookupIdentifierImpl id, T item) throws OperationIncompleteException;

    @NotNull Boolean delete(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    @NotNull List<Pair<String, T>> list() throws OperationIncompleteException;

    @NotNull Boolean contains(DataLookupIdentifierImpl id) throws OperationIncompleteException;

    @NotNull Integer size();
    void cleanup();
}
