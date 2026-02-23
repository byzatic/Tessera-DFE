package io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.observability.PrometheusMetricsAgent;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.storage.Storage;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager implements StorageManagerInterface {
    private final static Logger logger = LoggerFactory.getLogger(StorageManager.class);

    /**
     * Node-scoped storages: GraphNodeRef -> (storageId -> storage)
     */
    private final Map<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeStorageMap =
            new ConcurrentHashMap<>();

    /**
     * Global storages: storageId -> storage
     */
    private final Map<String, StorageInterface<DataValueInterface>> globalStorageMap =
            new ConcurrentHashMap<>();

    private final FullProjectRepository fullProjectRepository;

    public StorageManager(@NotNull FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        this.fullProjectRepository = fullProjectRepository;

        if (!Configuration.INITIALIZE_STORAGE_BY_REQUEST) {
            for (StoragesItem storageGlobal : fullProjectRepository.getGlobal().getStorages()) {
                if (storageGlobal.getIdName() == null || Objects.equals(storageGlobal.getIdName(), "")) {
                    throw new OperationIncompleteException("Global storage should have name -> " + storageGlobal);
                }
                String storageId = storageGlobal.getIdName();
                initializeGlobalStorage(storageId);
            }

            for (GraphNodeRef graphNodeRef : fullProjectRepository.listGraphNodeRef()) {
                for (io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem storageNode
                        : fullProjectRepository.getNodeGlobal(graphNodeRef).getStorages()) {
                    String storageName = storageNode.getIdName();
                    if (storageName == null || Objects.equals(storageName, "")) {
                        throw new OperationIncompleteException("Node storage should have name -> " + storageNode);
                    }
                    initializeNodeStorage(graphNodeRef, storageName);
                }
            }
        }

        // publish initial snapshot (best-effort, must not fail ctor)
        if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull GraphNodeRef graphNodeRef,
                                                 @NotNull String storageId,
                                                 @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}",
                graphNodeRef, storageId, storageItemId);

        try {
            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            if (storage.contains(storageItemId)) {
                DataValueInterface storageItem = storage.read(storageItemId);
                logger.debug("getItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is storageItem -> {}",
                        graphNodeRef, storageId, storageItemId, storageItem);
                return storageItem;
            }

            String errMessage = "Item with ID " + storageItemId + " was not found in storage " + storage.getStorageId();
            logger.error(errMessage);
            throw new OperationIncompleteException(errMessage);

        } catch (OperationIncompleteException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public void putItemToStorage(@NotNull GraphNodeRef graphNodeRef,
                                 @NotNull String storageId,
                                 @NotNull DataLookupIdentifierImpl dataLookupIdentifierInterface,
                                 @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        try {
            logger.debug("putItemToStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {} storageItem -> {}",
                    graphNodeRef, storageId, dataLookupIdentifierInterface, storageItem);

            StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
            if (storage.contains(dataLookupIdentifierInterface)) {
                storage.delete(dataLookupIdentifierInterface);
            }
            storage.create(dataLookupIdentifierInterface, storageItem);

            logger.debug("putItemToStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} dataLookupIdentifierInterface -> {} storageItem -> {} is complete",
                    graphNodeRef, storageId, dataLookupIdentifierInterface, storageItem);

        } catch (OperationIncompleteException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        } finally {
            if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
        }
    }

    private StorageInterface<DataValueInterface> searchNodeStorage(GraphNodeRef graphNodeRef, String storageId)
            throws OperationIncompleteException {

        if (!fullProjectRepository.isNodeStorageDeclaration(graphNodeRef, storageId)) {
            String errMessage = "No such Node " + graphNodeRef.getNodeUUID() + " storage " + storageId + " defined in ConfigProject";
            logger.error(errMessage);
            throw new OperationIncompleteException(errMessage);
        }

        if (!isNodeStorageExists(graphNodeRef, storageId)) {
            initializeNodeStorage(graphNodeRef, storageId);
            if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
        }

        return nodeStorageMap.get(graphNodeRef).get(storageId);
    }

    private void initializeNodeStorage(GraphNodeRef graphNodeRef, String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> storage = new Storage<>(storageId);
        if (nodeStorageMap.containsKey(graphNodeRef)) {
            nodeStorageMap.get(graphNodeRef).put(storageId, storage);
        } else {
            Map<String, StorageInterface<DataValueInterface>> newNodeMap = new ConcurrentHashMap<>();
            newNodeMap.put(storageId, storage);
            nodeStorageMap.put(graphNodeRef, newNodeMap);
        }
        logger.debug("Node {} storage {} created", graphNodeRef.getNodeUUID(), storageId);
    }

    private Boolean isNodeStorageExists(GraphNodeRef graphNodeRef, String storageId) {
        boolean result = nodeStorageMap.containsKey(graphNodeRef)
                && nodeStorageMap.get(graphNodeRef).containsKey(storageId);
        logger.debug("Is node {} storage {} exists: {}", graphNodeRef, storageId, result);
        return result;
    }

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull GraphNodeRef graphNodeRef,
                                @NotNull String storageId,
                                @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {}",
                graphNodeRef, storageId, storageItemId);

        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        boolean result = storage.contains(storageItemId);

        logger.debug("isDataExists (NODE STORAGE) graphNodeRef -> {} storageId -> {} storageItemId -> {} is {}",
                graphNodeRef, storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull GraphNodeRef graphNodeRef,
                                                                               @NotNull String storageId) throws OperationIncompleteException {
        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {}",
                graphNodeRef, storageId);

        StorageInterface<DataValueInterface> storage = searchNodeStorage(graphNodeRef, storageId);
        List<Pair<String, DataValueInterface>> result = storage.list();

        logger.debug("listItemFromStorage (NODE STORAGE) graphNodeRef -> {} storageId -> {} is {}",
                graphNodeRef, storageId, result);
        return result;
    }

    @NotNull
    @Override
    public DataValueInterface getItemFromStorage(@NotNull String storageId,
                                                 @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {}",
                storageId, storageItemId);

        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        if (storage.contains(storageItemId)) {
            DataValueInterface storageItem = storage.read(storageItemId);
            logger.debug("getItemFromStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is storageItem -> {}",
                    storageId, storageItemId, storageItem);
            return storageItem;
        }

        String errMessage = "Item with ID " + storageItemId + " was not found in storage " + storage.getStorageId();
        logger.error(errMessage);
        throw new OperationIncompleteException(errMessage);
    }

    @Override
    public void putItemToStorage(@NotNull String storageId,
                                 @NotNull DataLookupIdentifierImpl storageItemId,
                                 @NotNull DataValueInterface storageItem) throws OperationIncompleteException {
        try {
            logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} storageItem -> {}",
                    storageId, storageItemId, storageItem);

            StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
            if (storage.contains(storageItemId)) {
                storage.delete(storageItemId);
            }
            storage.create(storageItemId, storageItem);

            logger.debug("putItemToStorage (GLOBAL STORAGE) storageId -> {} storageItemId -> {} storageItem -> {} is complete",
                    storageId, storageItemId, storageItem);

        } finally {
            if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
        }
    }

    private StorageInterface<DataValueInterface> searchGlobalStorage(@NotNull String storageId)
            throws OperationIncompleteException {

        if (!globalStorageMap.containsKey(storageId)) {
            String errMessage = "No such Global storage " + storageId + " defined in ConfigProject";
            logger.error(errMessage);
            logger.error("{}", globalStorageMap);
            throw new OperationIncompleteException(errMessage);
        }

        if (!isGlobalStorageExists(storageId)) {
            initializeGlobalStorage(storageId);
            if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
        }

        return globalStorageMap.get(storageId);
    }

    private void initializeGlobalStorage(String storageId) throws OperationIncompleteException {
        StorageInterface<DataValueInterface> storage = new Storage<>(storageId);
        globalStorageMap.put(storageId, storage);
        logger.debug("Global storage {} created", storageId);
    }

    private Boolean isGlobalStorageExists(String storageId) {
        boolean result = globalStorageMap.containsKey(storageId);
        logger.debug("Is global storage {} exists: {}", storageId, result);
        return result;
    }

    @NotNull
    @Override
    public Boolean isDataExists(@NotNull String storageId,
                                @NotNull DataLookupIdentifierImpl storageItemId) throws OperationIncompleteException {
        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {}",
                storageId, storageItemId);

        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        boolean result = storage.contains(storageItemId);

        logger.debug("isDataExists (GLOBAL STORAGE) storageId -> {} storageItemId -> {} is {}",
                storageId, storageItemId, result);
        return result;
    }

    @Override
    public @NotNull List<Pair<String, DataValueInterface>> listItemFromStorage(@NotNull String storageId)
            throws OperationIncompleteException {
        logger.debug("listItemFromStorage (GLOBAL STORAGE) storageId -> {}",
                storageId);

        StorageInterface<DataValueInterface> storage = searchGlobalStorage(storageId);
        List<Pair<String, DataValueInterface>> result = storage.list();

        logger.debug("listItemFromStorage (GLOBAL STORAGE) storageId -> {} is {}",
                storageId, result);
        return result;
    }

    @Override
    public void cleanupNodeStorages() throws OperationIncompleteException {
        try {
            for (Map.Entry<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeStorageMapEntry : nodeStorageMap.entrySet()) {
                for (Map.Entry<String, StorageInterface<DataValueInterface>> storageMapEntry : nodeStorageMapEntry.getValue().entrySet()) {
                    storageMapEntry.getValue().cleanup();
                }
            }
        } finally {
            if (Configuration.PUBLISH_STORAGE_ANALYTICS) publishStorageMetricsSafe();
        }
    }

    /**
     * Best-effort publish storage metrics snapshot.
     * Metrics must never break storage operations.
     */
    private void publishStorageMetricsSafe() {
        try {
            publishStorageMetrics();
        } catch (Throwable t) {
            logger.debug("publishStorageMetrics failed: {}", t.toString());
        }
    }

    /**
     * Compute and publish aggregated storage sizes.
     *
     * - global scope: counts storages + total items in globalStorageMap
     * - node scope: counts storages + total items across ALL nodes in nodeStorageMap
     * - per storage_id: aggregated by storageId (no per-node labels)
     */
    private void publishStorageMetrics() {
        // If metrics agent isn't started yet, don't do any work
        PrometheusMetricsAgent agent;
        try {
            agent = PrometheusMetricsAgent.getInstance();
        } catch (Throwable t) {
            return;
        }

        long globalStorages = globalStorageMap.size();
        long nodeStorages = 0L;

        long globalItems = 0L;
        long nodeItems = 0L;

        Map<String, Long> globalById = new ConcurrentHashMap<>();
        Map<String, Long> nodeById = new ConcurrentHashMap<>();

        // global storages
        for (Map.Entry<String, StorageInterface<DataValueInterface>> e : globalStorageMap.entrySet()) {
            String storageId = e.getKey();
            StorageInterface<DataValueInterface> s = e.getValue();
            int sz = (s != null) ? s.size() : 0;

            globalItems += sz;
            globalById.merge(storageId, (long) sz, Long::sum);
        }

        // node storages (aggregate across nodes)
        for (Map.Entry<GraphNodeRef, Map<String, StorageInterface<DataValueInterface>>> nodeEntry : nodeStorageMap.entrySet()) {
            Map<String, StorageInterface<DataValueInterface>> storages = nodeEntry.getValue();
            if (storages == null) continue;

            nodeStorages += storages.size();

            for (Map.Entry<String, StorageInterface<DataValueInterface>> e : storages.entrySet()) {
                String storageId = e.getKey();
                StorageInterface<DataValueInterface> s = e.getValue();
                int sz = (s != null) ? s.size() : 0;

                nodeItems += sz;
                nodeById.merge(storageId, (long) sz, Long::sum);
            }
        }

        // publish snapshot (agent will throw if not started)
        agent.publishStorageSnapshot(
                globalStorages,
                nodeStorages,
                globalItems,
                nodeItems,
                globalById,
                nodeById
        );
    }
}