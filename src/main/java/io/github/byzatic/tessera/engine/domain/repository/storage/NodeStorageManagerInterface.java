package io.github.byzatic.tessera.engine.domain.repository.storage;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface NodeStorageManagerInterface {
    @NotNull DataValueInterface getItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface) throws OperationIncompleteException;

    void putItemToStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException;

    @NotNull Boolean isDataExists(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException;

    @NotNull List<Map.Entry<String, DataValueInterface>> listItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId) throws OperationIncompleteException;
}
