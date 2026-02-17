package io.github.byzatic.tessera.engine.domain.repository.storage;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface GlobalStorageManagerInterface {
    @NotNull DataValueInterface getItemFromStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface) throws OperationIncompleteException;

    void putItemToStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl dataLookupIdentifierInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException;

    @NotNull Boolean isDataExists(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException;

    @NotNull List<Map.Entry<String, DataValueInterface>> listItemFromStorage(@NotNull String storageId) throws OperationIncompleteException;
}
