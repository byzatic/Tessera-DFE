package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.storage;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.StorageInterface;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Storage<T extends DataValueInterface> implements StorageInterface<T> {
    private final static Logger logger = LoggerFactory.getLogger(Storage.class);
    private String storageId = null;
    private Map<String, T> storage = new ConcurrentHashMap<>();

    public Storage(@NotNull String storageId) throws OperationIncompleteException {
        if (storageId == null) throw new OperationIncompleteException("Storage Id should be not null");
        this.storageId = storageId;
    }

    public Storage(@NotNull String storageId, @NotNull Map<String, T> storageMap) throws OperationIncompleteException {
        if (storageId == null) throw new OperationIncompleteException("Storage Id should be not null");
        if (storage == null) throw new OperationIncompleteException("Storage should be not null");
        this.storageId = storageId;
        this.storage = storageMap;
    }

    public Storage(@NotNull Storage<T> storage) throws OperationIncompleteException {
        if (storage == null) throw new OperationIncompleteException("Storage should be not null");
        this.storageId = storage.storageId;
        this.storage = storage.storage;
    }

    public Storage(@NotNull String storageId, @NotNull Storage<T> storage) throws OperationIncompleteException {
        if (storageId == null) throw new OperationIncompleteException("Storage Id should be not null");
        if (storage == null) throw new OperationIncompleteException("Storage should be not null");
        this.storageId = storageId;
        this.storage = storage.storage;
    }

    @Contract("null -> fail")
    private @NotNull String getId(DataLookupIdentifierImpl id) throws OperationIncompleteException {
        if (id == null) throw new OperationIncompleteException(DataLookupIdentifierImpl.class.getSimpleName() + " should be not null");
        String temp = id.getDataLookupIdentifier();
        if (temp == null) throw new OperationIncompleteException(DataLookupIdentifierImpl.class.getSimpleName() + ".id should be not null");
        return temp;
    }

    @Override
    public @NotNull String getStorageId() {
        return storageId;
    }

    @Override
    public void create(@NotNull DataLookupIdentifierImpl storageItemIdI, @NotNull T item) throws OperationIncompleteException {
        if (item == null) throw new IllegalArgumentException("Item must not be null");
        String id = getId(storageItemIdI);
        if (storage.containsKey(id)) {
            throw new OperationIncompleteException("Item with ID already exists: " + id);
        }
        storage.put(id, item);
    }

    @Override
    public @NotNull T read(@NotNull DataLookupIdentifierImpl storageItemIdI) throws OperationIncompleteException {
        String id = getId(storageItemIdI);
        return storage.get(id);
    }

    @Override
    public @NotNull Boolean update(@NotNull DataLookupIdentifierImpl storageItemIdI, @NotNull T item) throws OperationIncompleteException {
        if (item == null) throw new IllegalArgumentException("Item must not be null");
        String id = getId(storageItemIdI);
        if (storage.containsKey(id)) {
            storage.put(id, item);
            return true;
        }
        return false;
    }

    @Override
    public  @NotNull Boolean delete(@NotNull DataLookupIdentifierImpl storageItemIdI) throws OperationIncompleteException {
        String id = getId(storageItemIdI);
        return storage.remove(id) != null;
    }

    @Override
    public  @NotNull List<Pair<String, T>> list() throws OperationIncompleteException {
        List<Pair<String, T>> storedPairs = new ArrayList<>();
        for (Map.Entry<String, T> set : storage.entrySet()) {
            storedPairs.add(new Pair<>(set.getKey(), set.getValue()));
        }
        return storedPairs;
    }

    @Override
    public @NotNull Boolean contains(@NotNull DataLookupIdentifierImpl storageItemIdI) throws OperationIncompleteException {
        String id = getId(storageItemIdI);
        return storage.containsKey(id);
    }

    @Override
    public @NotNull Integer size() {
        return storage.size();
    }

    @Override
    public void cleanup() {
        storage.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Storage<?> storage1 = (Storage<?>) o;
        return Objects.equals(storageId, storage1.storageId) && Objects.equals(storage, storage1.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageId, storage);
    }

    @Override
    public String toString() {
        return "Storage{" +
                "storageId='" + storageId + '\'' +
                ", storage=" + storage +
                '}';
    }
}
