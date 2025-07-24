package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;

import java.util.Map;

public interface PipelineDaoInterface {
    Map<GraphNodeRef, NodePipeline> load(String projectName) throws OperationIncompleteException;
}
