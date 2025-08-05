package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikePipelineRepositoryInterface;

import java.util.HashMap;
import java.util.Map;

public class JpaLikePipelineRepository implements JpaLikePipelineRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(JpaLikePipelineRepository.class);
    private final PipelineDaoInterface pipelineDao;
    private final String projectName;
    private final Map<GraphNodeRef, NodePipeline> nodePipelineMap = new HashMap<>();

    public JpaLikePipelineRepository(String projectName, PipelineDaoInterface pipelineDao) throws OperationIncompleteException {
        try {
            if (projectName == null) {
                String errMessage = "ProjectName can not be null";
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
            this.projectName = projectName;
            this.pipelineDao = pipelineDao;
            load();
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load() throws OperationIncompleteException {
        try {
            nodePipelineMap.putAll(pipelineDao.load(projectName));
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public NodePipeline getPipeline(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Requested {} by {} -> {}", NodePipeline.class.getSimpleName(), GraphNodeRef.class.getSimpleName(), graphNodeRef);
        NodePipeline result = null;
        if (nodePipelineMap.containsKey(graphNodeRef)) {
            result = nodePipelineMap.get(graphNodeRef);
        } else {
            String errMessage = "Repository " + JpaLikePipelineRepository.class.getSimpleName() + " not contains object " + NodePipeline.class.getSimpleName() + " requested by " + graphNodeRef;
            logger.error(errMessage);
            logger.error("Repository -> {}", nodePipelineMap);
            throw new OperationIncompleteException(errMessage);
        }
        logger.debug("Returns requested {}", NodePipeline.class.getSimpleName());
        logger.trace("Returns requested {} -> {}", NodePipeline.class.getSimpleName(), result);
        return result;
    }

    @Override
    public void reload() throws OperationIncompleteException {
        try {
            nodePipelineMap.clear();
            nodePipelineMap.putAll(pipelineDao.load(projectName));
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
