package io.github.byzatic.tessera.engine.domain.repository.storage;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;

import java.util.List;

public interface GlobalStorageManagerInterface {
    @NotNull DataValueInterface getItemFromStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface) throws OperationIncompleteException;

    void putItemToStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl dataLookupIdentifierInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException;

    @NotNull Boolean isDataExists(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException;

    @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull String storageId) throws OperationIncompleteException;
}
