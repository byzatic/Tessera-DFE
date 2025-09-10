package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_manager.StructureManagerInterface;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathManager implements PathManagerInterface {
    private final static Logger logger= LoggerFactory.getLogger(PathManager.class);
    private final Map<GraphNodeRef, Path> nodeGlobalStorageMap = new ConcurrentHashMap<>();
    private Path projectGlobalStorage = null;

    public PathManager(JpaLikeNodeRepositoryInterface nodeRepository, StructureManagerInterface structureManager) throws OperationIncompleteException {
        this.projectGlobalStorage = structureManager.getProjectStructure().getProjectConfigurationFilesFolder();
        try {
            for (GraphNodeRef graphNodeRef : nodeRepository.getAllGraphNodeRef()) {
                Path nodeGlobalStorage = structureManager.getNodeStructure(graphNodeRef).getNodeConfigurationFilesFolder();
                nodeGlobalStorageMap.put(graphNodeRef, nodeGlobalStorage);
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public Path getStoragePathByGraphNodeRef(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        Path result = null;
        if (nodeGlobalStorageMap.containsKey(graphNodeRef)) {
            result = nodeGlobalStorageMap.get(graphNodeRef);
        } else {
            String errorMessage = "PathManager not contains path to jpa_like_node_repository"+graphNodeRef;
            logger.error(errorMessage);
            throw new OperationIncompleteException(errorMessage);
        }
        return result;
    }

    @Override
    public Path getProjectGlobalStorage() {
        return projectGlobalStorage;
    }
}
