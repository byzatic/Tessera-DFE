package io.github.byzatic.tessera.engine.domain.repository;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;

public interface JpaLikePipelineRepositoryInterface extends ResourcesInterface {
    NodePipeline getPipeline(GraphNodeRef graphNodeRef) throws OperationIncompleteException;

    void reload();
}
