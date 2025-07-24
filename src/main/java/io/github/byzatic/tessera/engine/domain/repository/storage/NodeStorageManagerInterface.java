package io.github.byzatic.tessera.engine.domain.repository.storage;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import ru.byzatic.metrics_core.mcg3_storageapi_lib.dto.DataValueInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;

import java.util.List;

public interface NodeStorageManagerInterface {
    @NotNull DataValueInterface getItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface) throws OperationIncompleteException;

    void putItemToStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataIdInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException;

    @NotNull Boolean isDataExists(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException;

    @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId) throws OperationIncompleteException;
}
