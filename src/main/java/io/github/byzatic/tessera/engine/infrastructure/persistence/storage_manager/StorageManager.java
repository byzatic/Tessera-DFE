package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager;

import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeNodeGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeProjectGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.storage.Storage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager implements StorageManagerInterface {
    private final static Logger logger = LoggerFactory.getLogger(StorageManager.class);
    private final Map<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeStorageMap = new ConcurrentHashMap<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>>();
    private final Map<String, StorageInterface<DataValueInterface>> globalStorageMap = new ConcurrentHashMap<>();
    private JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository = null;
    private JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository = null;

    public StorageManager(@NotNull JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository, @NotNull JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository, @NotNull JpaLikeNodeRepositoryInterface nodeRepository) throws OperationIncompleteException {
        this.nodeGlobalRepository = nodeGlobalRepository;
        this.projectGlobalRepository = projectGlobalRepository;
        if (! Configuration.INITIALIZE_STORAGE_BY_REQUEST) {
            for (StoragesItem storageGlobal : projectGlobalRepository.getProjectGlobal().getStorages()) {
                if (storageGlobal.getIdName() == null || Objects.equals(storageGlobal.getIdName(), "")) {
                    throw new OperationIncompleteException("Global storage should have name -> " + storageGlobal);
                }
                String storageId = storageGlobal.getIdName();
                initializeGlobalStorage(storageId);
            }
            for (GraphNodeRef graphNodeRef : nodeRepository.getAllGraphNodeRef()) {
                for (io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem storageNode : nodeGlobalRepository.getNodeGlobal(graphNodeRef).getStorages()) {
                    String storageName = storageNode.getIdName();
                    if (storageName == null || Objects.equals(storageName, "")) {
                        throw new OperationIncompleteException("Node storage should have name -> " + storageNode);
                    }
                    initializeNodeStorage(graphNodeRef, storageName);
                }
            }
        }
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}", graphNodeRef, storageId, storageItemId);
        DataValueInterface storageItem = null;
        try {
            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            if (storage.contains(storageItemId)) {
                storageItem = storage.read(storageItemId);
            } else {
                String errMessage = "Item with ID " + storageItemId + " was not found in storage " + storage.getStorageId();
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
            logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is storageItem -> {}", graphNodeRef, storageId, storageItemId, storageItem);
            return storageItem;
        } catch (Exception e ) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public void putItemToStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataLookupIdentifierInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        try {
            logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {} storageItem -> {}", graphNodeRef, storageId, dataLookupIdentifierInterface, storageItem);
            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            if (storage.contains(dataLookupIdentifierInterface)) {
                storage.delete(dataLookupIdentifierInterface);
                storage.create(dataLookupIdentifierInterface, storageItem);
            } else {
                storage.create(dataLookupIdentifierInterface, storageItem);
            }
            logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {} storageItem -> {} is complete", graphNodeRef, storageId, dataLookupIdentifierInterface, storageItem);
        } catch (Exception e ) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private StorageInterface<DataValueInterface> searchNodeStorage(GraphNodeRef graphNodeRef, String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> result;

        if (! nodeGlobalRepository.isStorageWithId(graphNodeRef, storageId)) {
            String errMessage = "No such Node " + graphNodeRef.getNodeUUID() + " storage " + storageId + " defined in ConfigProject";
            logger.error(errMessage);
            throw new OperationIncompleteException(errMessage);
        }

        if (! isNodeStorageExists(graphNodeRef, storageId)) {
            initializeNodeStorage(graphNodeRef, storageId);
        }

        result = nodeStorageMap.get(graphNodeRef).get(storageId);

        return result;
    }

    private void initializeNodeStorage(GraphNodeRef graphNodeRef, String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> storage = new Storage<>(storageId);
        if (nodeStorageMap.containsKey(graphNodeRef)) {
            Map<String, StorageInterface<DataValueInterface>> existedNodeMap = nodeStorageMap.get(graphNodeRef);
            existedNodeMap.put(storageId, storage);
        } else {
            Map<String, StorageInterface<DataValueInterface>> newNodeMap = new ConcurrentHashMap<>();
            newNodeMap.put(storageId, storage);
            nodeStorageMap.put(graphNodeRef, newNodeMap);
        }
        logger.debug("Node {} storage {} created", graphNodeRef.getNodeUUID(), storageId);
    }

    private Boolean isNodeStorageExists(GraphNodeRef graphNodeRef, String storageId) {
        Boolean result = Boolean.FALSE;
        if (nodeStorageMap.containsKey(graphNodeRef)) {
            if (nodeStorageMap.get(graphNodeRef).containsKey(storageId)) {
                result = Boolean.TRUE;
            }
        }
        logger.debug("Is jpa_like_node_repository {} storage {} exists: {}", graphNodeRef, storageId, result);
        return result;
    }

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}", graphNodeRef, storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        Boolean result = null;
        if (storage.contains(storageItemId)) {
            result = Boolean.TRUE;
        } else {
            result = Boolean.FALSE;
        }
        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is {}", graphNodeRef, storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId) throws OperationIncompleteException {
        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {}", graphNodeRef, storageId);
        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        List<Pair<String, DataValueInterface>> result = storage.list();
        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} is {}", graphNodeRef, storageId, result);
        return result;
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {}", storageId, storageItemId);
        DataValueInterface storageItem = null;

        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        if (storage.contains(storageItemId)) {
            storageItem = storage.read(storageItemId);
        } else {
            String errMessage = "Item with ID " + storageItemId + " was not found in storage " + storage.getStorageId();
            logger.error(errMessage);
            throw new OperationIncompleteException(errMessage);
        }
        logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is storageItem -> {}", storageId, storageItemId, storageItem);
        return storageItem;
    }

    @Override
    public void putItemToStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId, @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} storageItem -> {}", storageId, storageItemId, storageItem);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        if (storage.contains(storageItemId)) {
            storage.delete(storageItemId);
            storage.create(storageItemId, storageItem);
        } else {
            storage.create(storageItemId, storageItem);
        }
        logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} storageItem -> {} is complete", storageId, storageItemId, storageItem);
    }

    private StorageInterface<DataValueInterface> searchGlobalStorage(@NotNull String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> result;

        if (! globalStorageMap.containsKey(storageId)) {
            String errMessage = "No such Global storage " + storageId + " defined in ConfigProject";
            logger.error(errMessage);
            logger.error("{}", globalStorageMap);
            throw new OperationIncompleteException(errMessage);
        }

        if (! isGlobalStorageExists(storageId)) {
            initializeGlobalStorage(storageId);
        }

        result = globalStorageMap.get(storageId);

        return result;
    }

    private void initializeGlobalStorage(String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> storage = new Storage<>(storageId);
        globalStorageMap.put(storageId, storage);
        logger.debug("Global storage {} created", storageId);
    }

    private Boolean isGlobalStorageExists(String storageId) {
        Boolean result = Boolean.FALSE;
        if (globalStorageMap.containsKey(storageId)) {
            result = Boolean.TRUE;
        }
        logger.debug("Is dto storage {} exists: {}", storageId, result);
        return result;
    }

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {}", storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        Boolean result = null;
        if (storage.contains(storageItemId)) {
            result = Boolean.TRUE;
        } else {
            result = Boolean.FALSE;
        }
        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is {}", storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull String storageId) throws OperationIncompleteException {
        logger.debug("listItemFromStorage (GLOBAL STORAGE) storageId -> {}", storageId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        List<Pair<String, DataValueInterface>> result = storage.list();
        logger.debug("listItemFromStorage (GLOBAL STORAGE) storageId -> {} is {}", storageId, result);
        return result;
    }

    @Override
    public void cleanupNodeStorages() throws OperationIncompleteException {
        for (Map.Entry<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeStorageMapEntry : nodeStorageMap.entrySet()) {
            for (Map.Entry<String, StorageInterface<DataValueInterface>> storageMapEntry : nodeStorageMapEntry.getValue().entrySet()) {
                storageMapEntry.getValue().cleanup();
            }
        }
    }
}
