package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;

import java.io.FileReader;
import java.nio.file.Path;

class SupportNodePipelineLoader {
    private final static Logger logger = LoggerFactory.getLogger(SupportNodePipelineLoader.class);
    private final static Gson gson = new Gson();

    public static NodePipeline load(Path fileNodePipeline) throws OperationIncompleteException {
        try {
            FileReader reader = new FileReader(fileNodePipeline.toFile());
            NodePipeline nodePipeline = gson.fromJson(reader, NodePipeline.class);
            logger.debug("Loaded {} from {}",NodePipeline.class.getSimpleName() , fileNodePipeline);
            logger.trace("Loaded {} from {} -> {}",NodePipeline.class.getSimpleName() , fileNodePipeline, nodePipeline);
            return nodePipeline;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
