package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.storageapi.dto.DataValueInterface;
import io.github.byzatic.tessera.storageapi.dto.StorageItem;
import io.github.byzatic.tessera.storageapi.storageapi.StorageApiInterface;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.model.DataLookupIdentifierImpl;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.storageapi.exceptions.MCg3ApiOperationIncompleteException;

import java.util.ArrayList;
import java.util.List;


public class StorageApi implements StorageApiInterface {
    private final static Logger logger= LoggerFactory.getLogger(io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.StorageApi.class);
    private final GraphNodeRef graphNodeRef;
    private final JpaLikeNodeRepositoryInterface nodeRepository;
    private StorageManagerInterface storageManager = null;

    public StorageApi(StorageManagerInterface storageManager, GraphNodeRef graphNodeRef, JpaLikeNodeRepositoryInterface nodeRepository) {
        this.storageManager = storageManager;
        this.graphNodeRef = graphNodeRef;
        this.nodeRepository = nodeRepository;
    }

    private GraphNodeRef getDownstreamGraphNodeRef(StorageItem storageItem) throws MCg3ApiOperationIncompleteException {
        try {
            String namingTag = "#NAMED";
            String downstreamNodeId = storageItem.getDownstreamName();
            ObjectsUtils.requireNonNull(downstreamNodeId, new IllegalArgumentException("Downstream node name should be NotNull"));
            logger.debug("Searching downstream GraphNodeRef by node ID {}", downstreamNodeId);
            GraphNodeRef localRequestNode = null;
            NodeItem nodeItem = nodeRepository.getNode(graphNodeRef);
            List<GraphNodeRef> graphNodeRefList = nodeItem.getDownstream();
            for (GraphNodeRef downstreamGraphNodeRef : graphNodeRefList) {
                NodeItem downstreamNodeItem = nodeRepository.getNode(downstreamGraphNodeRef);
                logger.debug("Processing search {} for {} and {}", downstreamNodeId, downstreamGraphNodeRef, downstreamNodeItem);
                if (downstreamNodeItem.getId().equals(downstreamNodeId)) {
                    localRequestNode = downstreamGraphNodeRef;
                    logger.debug("Processing search {} for {} success", downstreamNodeId, downstreamGraphNodeRef);
                    break;
                } else if (downstreamNodeItem.getId().equals(namingTag) && downstreamNodeItem.getName().equals(downstreamNodeId)) {
                    localRequestNode = downstreamGraphNodeRef;
                    logger.debug("Processing search {} for {} success", downstreamNodeId, downstreamGraphNodeRef);
                    break;
                } else {
                    logger.debug("Processing search {} for {} moving next", downstreamNodeId, downstreamGraphNodeRef);
                }
            }
            logger.debug("Searching downstream GraphNodeRef by node ID {} complete; result is {}", downstreamNodeId, localRequestNode);
            ObjectsUtils.requireNonNull(localRequestNode, new MCg3ApiOperationIncompleteException("Node with name "+storageItem.getDownstreamName()+" was not found"));
            return localRequestNode;
        } catch (Exception e) {
            throw new MCg3ApiOperationIncompleteException(e);
        }
    }

    @Override
    public StorageItem getStorageObject(StorageItem storageItem) throws MCg3ApiOperationIncompleteException {
        StorageItem result = null;
        DataValueInterface storageManagerResult = null;
        StorageItem.ScopeType scope = storageItem.getScope();

        try {
            switch (scope) {
                case LOCAL -> {
                    logger.debug("Get data from LOCAL scope");
                    storageManagerResult = storageManager.getItemFromStorage(
                            graphNodeRef,
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );
                    logger.debug("Get data from LOCAL scope complete");
                    break;
                }
                case GLOBAL -> {
                    logger.debug("Get data from GLOBAL scope");
                    storageManagerResult = storageManager.getItemFromStorage(
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );
                    logger.debug("Get data from GLOBAL scope complete");
                    break;
                }
                case DOWNSTREAM -> {
                    logger.debug("Get data from DOWNSTREAM scope");
                    GraphNodeRef localRequestNode = getDownstreamGraphNodeRef(storageItem);
                    storageManagerResult = storageManager.getItemFromStorage(
                            localRequestNode,
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );

                    logger.debug("Get data from DOWNSTREAM scope complete");
                    break;
                }
                default -> throw new MCg3ApiOperationIncompleteException("Unknown scope");
            }

            result = StorageItem.newBuilder()
                    .setScope(storageItem.getScope())
                    .setDownstreamName(storageItem.getDownstreamName())
                    .setStorageId(storageItem.getStorageId())
                    .setDataId(storageItem.getDataId())
                    .setDataValue(storageManagerResult)
                    .build();
        } catch (Exception e) {
            throw new MCg3ApiOperationIncompleteException(e);
        }
        return result;
    }

    @Override
    public void putStorageObject(StorageItem storageItem) throws MCg3ApiOperationIncompleteException {
        StorageItem.ScopeType scope = storageItem.getScope();
        try {
            switch (scope) {
                case LOCAL -> {
                    storageManager.putItemToStorage(
                            graphNodeRef,
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build(),
                            storageItem.getDataValue()
                    );
                    break;
                }
                case GLOBAL -> {
                    storageManager.putItemToStorage(
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build(),
                            storageItem.getDataValue()
                    );
                    break;
                }
                case DOWNSTREAM -> throw new MCg3ApiOperationIncompleteException("Can't put to downstream");
                default -> throw new MCg3ApiOperationIncompleteException("Unknown scope");
            }
        } catch (Exception e) {
            throw new MCg3ApiOperationIncompleteException(e);
        }
    }

    @Override
    public Boolean isDataByIdExists(StorageItem storageItem) throws MCg3ApiOperationIncompleteException {
        Boolean result = null;
        StorageItem.ScopeType scope = storageItem.getScope();
        try {
            switch (scope) {
                case LOCAL -> {
                    result = storageManager.isDataExists(
                            graphNodeRef,
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );
                }
                case GLOBAL -> {
                    result = storageManager.isDataExists(
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );
                }
                case DOWNSTREAM -> {
                    GraphNodeRef localRequestNode = getDownstreamGraphNodeRef(storageItem);
                    result = storageManager.isDataExists(
                            localRequestNode,
                            storageItem.getStorageId(),
                            DataLookupIdentifierImpl.newBuilder().dataId(storageItem.getDataId()).build()
                    );
                    break;
                }
                default -> throw new MCg3ApiOperationIncompleteException("Unknown scope");
            }
        } catch (Exception e) {
            throw new MCg3ApiOperationIncompleteException(e);
        }
        return result;
    }

    @Override
    public List<StorageItem> listStorageObjects(StorageItem storageItem) throws MCg3ApiOperationIncompleteException {
        StorageItem.ScopeType scope = storageItem.getScope();
        List<StorageItem> foundedStorageItems = new ArrayList<>();
        List<Pair<String, DataValueInterface>> listItemFromStorage;
        try {
            switch (scope) {
                case LOCAL -> {
                    listItemFromStorage = storageManager.listItemFromStorage(
                            graphNodeRef,
                            storageItem.getStorageId()
                    );
                    break;
                }
                case GLOBAL -> {
                    listItemFromStorage = storageManager.listItemFromStorage(
                            storageItem.getStorageId()
                    );
                    break;
                }
                case DOWNSTREAM -> {
                    GraphNodeRef localRequestNode = getDownstreamGraphNodeRef(storageItem);
                    listItemFromStorage = storageManager.listItemFromStorage(
                            localRequestNode,
                            storageItem.getStorageId()
                    );
                    break;
                }
                default -> throw new MCg3ApiOperationIncompleteException("Unknown scope");
            }
            for (Pair<String, DataValueInterface> data : listItemFromStorage) {
                foundedStorageItems.add(
                        StorageItem.newBuilder()
                                .setScope(storageItem.getScope())
                                .setDownstreamName(storageItem.getDownstreamName())
                                .setStorageId(storageItem.getStorageId())
                                .setDataId(data.getFirst())
                                .setDataValue(data.getSecond())
                                .build()
                );
            }
            return foundedStorageItems;
        } catch (Exception e) {
            throw new MCg3ApiOperationIncompleteException(e);
        }
    }

}

