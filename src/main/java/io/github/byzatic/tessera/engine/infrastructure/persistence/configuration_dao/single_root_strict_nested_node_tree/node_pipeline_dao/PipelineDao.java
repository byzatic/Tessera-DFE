package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.NodeToGNRContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.PipelineDaoInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PipelineDao implements PipelineDaoInterface {
    private final static Logger logger = LoggerFactory.getLogger(PipelineDao.class);
    private final StructureControllerInterface structureManager;

    public PipelineDao(StructureControllerInterface structureManager) {
        this.structureManager = structureManager;
    }

    @Override
    public Map<GraphNodeRef, NodePipeline> load(String projectName, NodeToGNRContainer nodeToGNRContainer) throws OperationIncompleteException {
        try {
            Map<GraphNodeRef, NodePipeline> nodePipelineMap = new HashMap<>();
            for (GraphNodeRef graphNodeRef : nodeToGNRContainer.getAllGraphNodeRef()) {
                String nodeId = graphNodeRef.getNodeUUID();
                if (nodeId == null) {
                    String errMessage = "Node id in " + graphNodeRef + " can not be null";
                    logger.error(errMessage);
                    throw new OperationIncompleteException(errMessage);
                }
                Path nodePath = structureManager.getNodeStructure(graphNodeRef, nodeToGNRContainer).getNodeFolder().resolve("pipeline.json");
                NodePipeline nodePipeline = SupportNodePipelineLoader.load(nodePath);
                if (nodePipelineMap.containsKey(graphNodeRef)) {
                    String errMessage = "Repository " + NodeToGNRContainer.class.getSimpleName() + " already contains object " + NodePipeline.class.getSimpleName() + " by identifier " + graphNodeRef;
                    logger.error(errMessage);
                    throw new OperationIncompleteException(errMessage);
                } else {
                    nodePipelineMap.put(graphNodeRef, nodePipeline);
                    logger.debug("Object {} saved by identifier {}", NodePipeline.class.getSimpleName(), graphNodeRef);
                    logger.trace("Object {} saved by identifier {}", nodePipeline, graphNodeRef);
                }
            }
            return nodePipelineMap;
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
