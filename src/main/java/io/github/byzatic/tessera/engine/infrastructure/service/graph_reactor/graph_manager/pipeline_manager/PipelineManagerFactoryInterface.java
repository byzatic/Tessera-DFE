package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;

import java.util.List;

public interface PipelineManagerFactoryInterface {
    PipelineManagerInterface getNewPipelineManager(GraphNodeRef currentExecutionNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef) throws OperationIncompleteException;
}
