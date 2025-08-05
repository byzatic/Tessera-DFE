package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.JpaLikePipelineRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JpaLikeNodeGlobalRepository implements JpaLikeNodeGlobalRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(JpaLikeNodeGlobalRepository.class);
    private final NodeGlobalDaoInterface nodeGlobalDaoInterface;
    private final String projectName;
    private final Map<GraphNodeRef, NodeGlobal> nodeGlobalMap = new HashMap<>();

    public JpaLikeNodeGlobalRepository(String projectName, NodeGlobalDaoInterface nodeGlobalDaoInterface) throws OperationIncompleteException {
        try {
            if (projectName == null) {
                String errMessage = "ProjectName can not be null";
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
            this.nodeGlobalDaoInterface = nodeGlobalDaoInterface;
            this.projectName = projectName;
            load(projectName);
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load(String projectName) throws OperationIncompleteException {
        try {
            nodeGlobalMap.putAll(nodeGlobalDaoInterface.load(projectName));
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public NodeGlobal getNodeGlobal(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Requested {} by {} -> {}", NodeGlobal.class.getSimpleName(), GraphNodeRef.class.getSimpleName(), graphNodeRef);
        NodeGlobal result = null;
        if (nodeGlobalMap.containsKey(graphNodeRef)) {
            result = nodeGlobalMap.get(graphNodeRef);
        } else {
            String errMessage = "Repository " + JpaLikePipelineRepository.class.getSimpleName() + " not contains object " + NodeGlobal.class.getSimpleName() + " requested by " + graphNodeRef;
            logger.error(errMessage);
            logger.error("Repository -> {}", nodeGlobalMap);
            throw new OperationIncompleteException(errMessage);
        }
        logger.debug("Returns requested {}", NodeGlobal.class.getSimpleName());
        logger.trace("Returns requested {} -> {}", NodeGlobal.class.getSimpleName(), result);
        return result;
    }

    @Override
    public Boolean isStorageWithId(GraphNodeRef graphNodeRef, String storageName) {
        boolean result = Boolean.FALSE;
        if (nodeGlobalMap.containsKey(graphNodeRef)) {
            for (StoragesItem storageItem : nodeGlobalMap.get(graphNodeRef).getStorages()) {
                if (Objects.equals(storageItem.getIdName(), storageName)) {
                    result = Boolean.TRUE;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void reload() throws OperationIncompleteException {
        try {
            nodeGlobalMap.clear();
            nodeGlobalMap.putAll(nodeGlobalDaoInterface.load(projectName));
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
