package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_global_dao;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.common.NodeToGNRContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.NodeGlobalDaoInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NodeGlobalDao implements NodeGlobalDaoInterface {
    private final static Logger logger = LoggerFactory.getLogger(NodeGlobalDao.class);
    private final StructureControllerInterface structureManager;

    public NodeGlobalDao(StructureControllerInterface structureManager) {
        this.structureManager = structureManager;
    }

    @Override
    public Map<GraphNodeRef, NodeGlobal> load(String projectName, NodeToGNRContainer nodeToGNRContainer) throws OperationIncompleteException {
        try {
            Map<GraphNodeRef, NodeGlobal> nodeGlobalMap = new HashMap<>();
            for (GraphNodeRef graphNodeRef : nodeToGNRContainer.getAllGraphNodeRef()) {
                Path nodePath = structureManager.getNodeStructure(graphNodeRef).getNodeFolder().resolve("global.json");
                NodeGlobal nodeGlobal = SupportNodeGlobalLoader.load(nodePath);
                if (nodeGlobalMap.containsKey(graphNodeRef)) {
                    String errMessage = "Repository " + NodeToGNRContainer.class.getSimpleName() + " already contains object " + NodeGlobal.class.getSimpleName() + " by identifier " + graphNodeRef;
                    logger.error(errMessage);
                    throw new OperationIncompleteException(errMessage);
                } else {
                    nodeGlobalMap.put(graphNodeRef, nodeGlobal);
                    logger.debug("Object {} saved by identifier {}", NodeGlobal.class.getSimpleName(), graphNodeRef);
                    logger.trace("Object {} saved by identifier {}", nodeGlobal, graphNodeRef);
                }
            }
            return nodeGlobalMap;
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
