package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.storage.Storage;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager implements StorageManagerInterface {
    private final static Logger logger = LoggerFactory.getLogger(StorageManager.class);
    private final Map<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeStorageMap = new ConcurrentHashMap<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>>();
    private final Map<String, StorageInterface<DataValueInterface>> globalStorageMap = new ConcurrentHashMap<>();
    private final FullProjectRepository fullProjectRepository;

    public StorageManager(@NotNull FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        this.fullProjectRepository = fullProjectRepository;
        if (!Configuration.INITIALIZE_STORAGE_BY_REQUEST) {
            // Eager initialization: pre-create all declared storages
            for (StoragesItem storageGlobal : fullProjectRepository.getGlobal().getStorages()) {
                if (storageGlobal.getIdName() == null || Objects.equals(storageGlobal.getIdName(), "")) {
                    throw new OperationIncompleteException("Global storage should have name -> " + storageGlobal);
                }
                String storageId = storageGlobal.getIdName();
                globalStorageMap.computeIfAbsent(storageId, k -> {
                    try {
                        logger.debug("Global storage {} created", storageId);
                        return new Storage<>(storageId);
                    } catch (OperationIncompleteException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            for (GraphNodeRef graphNodeRef : fullProjectRepository.listGraphNodeRef()) {
                // Get or create the node map atomically
                Map<String, StorageInterface<DataValueInterface>> nodeMap = nodeStorageMap.computeIfAbsent(
                        graphNodeRef, k -> new ConcurrentHashMap<>());

                for (io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem storageNode : fullProjectRepository.getNodeGlobal(graphNodeRef).getStorages()) {
                    String storageName = storageNode.getIdName();
                    if (storageName == null || Objects.equals(storageName, "")) {
                        throw new OperationIncompleteException("Node storage should have name -> " + storageNode);
                    }
                    nodeMap.computeIfAbsent(storageName, k -> {
                        try {
                            logger.debug("Node {} storage {} created", graphNodeRef.getNodeUUID(), storageName);
                            return new Storage<>(storageName);
                        } catch (OperationIncompleteException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}", graphNodeRef, storageId, storageItemId);
        try {
            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            DataValueInterface storageItem = storage.read(storageItemId);
            logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is storageItem -> {}", graphNodeRef, storageId, storageItemId, storageItem);
            return storageItem;
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public void putItemToStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl dataLookupIdentifierInterface, @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        try {
            logger.debug("putItemToStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {}", graphNodeRef, storageId, dataLookupIdentifierInterface);
            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            // Use update (upsert) instead of delete+create to reduce operations
            boolean updated = storage.update(dataLookupIdentifierInterface, storageItem);
            if (!updated) {
                // Item didn't exist, create it
                storage.create(dataLookupIdentifierInterface, storageItem);
            }
            logger.debug("putItemToStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {} is complete", graphNodeRef, storageId, dataLookupIdentifierInterface);
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private StorageInterface<DataValueInterface> searchNodeStorage(GraphNodeRef graphNodeRef, String storageId) throws OperationIncompleteException {
        if (!fullProjectRepository.isNodeStorageDeclaration(graphNodeRef, storageId)) {
            String errMessage = "No such Node " + graphNodeRef.getNodeUUID() + " storage " + storageId + " defined in ConfigProject";
            logger.error(errMessage);
            throw new OperationIncompleteException(errMessage);
        }

        // Use computeIfAbsent for atomic initialization of the node storage map
        Map<String, StorageInterface<DataValueInterface>> nodeMap = nodeStorageMap.computeIfAbsent(graphNodeRef,
                k -> new ConcurrentHashMap<>());

        // Use computeIfAbsent for atomic initialization of the specific storage
        return nodeMap.computeIfAbsent(storageId, k -> {
            try {
                logger.debug("Node {} storage {} created", graphNodeRef.getNodeUUID(), storageId);
                return new Storage<>(storageId);
            } catch (OperationIncompleteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Note: initializeNodeStorage and isNodeStorageExists removed - now handled atomically by computeIfAbsent in searchNodeStorage

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}", graphNodeRef, storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        Boolean result = storage.contains(storageItemId);
        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is {}", graphNodeRef, storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Map.Entry<String, DataValueInterface>> listItemFromStorage(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageId) throws OperationIncompleteException {
        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {}", graphNodeRef, storageId);
        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        List<Map.Entry<String, DataValueInterface>> result = storage.list();
        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} is {}", graphNodeRef, storageId, result);
        return result;
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {}", storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        DataValueInterface storageItem = storage.read(storageItemId);
        logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is storageItem -> {}", storageId, storageItemId, storageItem);
        return storageItem;
    }

    @Override
    public void putItemToStorage(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId, @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {}", storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        // Use update (upsert) instead of delete+create to reduce operations
        boolean updated = storage.update(storageItemId, storageItem);
        if (!updated) {
            // Item didn't exist, create it
            storage.create(storageItemId, storageItem);
        }
        logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is complete", storageId, storageItemId);
    }

    private StorageInterface<DataValueInterface> searchGlobalStorage(@NotNull String storageId) throws OperationIncompleteException {
        // When INITIALIZE_STORAGE_BY_REQUEST is false, we validate against the project repository
        // but still use lazy initialization via computeIfAbsent
        if (!Configuration.INITIALIZE_STORAGE_BY_REQUEST) {
            boolean storageDeclared = false;
            for (StoragesItem storageGlobal : fullProjectRepository.getGlobal().getStorages()) {
                if (storageId.equals(storageGlobal.getIdName())) {
                    storageDeclared = true;
                    break;
                }
            }
            if (!storageDeclared) {
                String errMessage = "No such Global storage " + storageId + " defined in ConfigProject";
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
        }

        // Use computeIfAbsent for atomic lazy initialization
        return globalStorageMap.computeIfAbsent(storageId, k -> {
            try {
                logger.debug("Global storage {} created", storageId);
                return new Storage<>(storageId);
            } catch (OperationIncompleteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull String storageId, @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {}", storageId, storageItemId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        Boolean result = storage.contains(storageItemId);
        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is {}", storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Map.Entry<String, DataValueInterface>> listItemFromStorage(@NotNull String storageId) throws OperationIncompleteException {
        logger.debug("listItemFromStorage (GLOBAL STORAGE) storageId -> {}", storageId);
        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        List<Map.Entry<String, DataValueInterface>> result = storage.list();
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
