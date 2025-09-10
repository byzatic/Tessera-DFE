package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_manager.StructureManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.JpaLikePipelineRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.PipelineDaoInterface;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PipelineDao implements PipelineDaoInterface {
    private final static Logger logger = LoggerFactory.getLogger(PipelineDao.class);
    private final StructureManagerInterface structureManager;
    private JpaLikeNodeRepositoryInterface nodeRepository = null;

    public PipelineDao(JpaLikeNodeRepositoryInterface nodeRepository, StructureManagerInterface structureManager) {
        this.nodeRepository = nodeRepository;
        this.structureManager = structureManager;
    }

    @Override
    public Map<GraphNodeRef, NodePipeline> load(String projectName) throws OperationIncompleteException {
        try {
            Map<GraphNodeRef, NodePipeline> nodePipelineMap = new HashMap<>();
            for (GraphNodeRef graphNodeRef : nodeRepository.getAllGraphNodeRef()) {
                String nodeId = graphNodeRef.getNodeUUID();
                if (nodeId == null) {
                    String errMessage = "Node id in " + graphNodeRef + " can not be null";
                    logger.error(errMessage);
                    throw new OperationIncompleteException(errMessage);
                }
                Path nodePath = structureManager.getNodeStructure(graphNodeRef).getNodeFolder().resolve("pipeline.json");
                NodePipeline nodePipeline = SupportNodePipelineLoader.load(nodePath);
                if (nodePipelineMap.containsKey(graphNodeRef)) {
                    String errMessage = "Repository " + JpaLikePipelineRepository.class.getSimpleName() + " already contains object " + NodePipeline.class.getSimpleName() + " by identifier " + graphNodeRef;
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
