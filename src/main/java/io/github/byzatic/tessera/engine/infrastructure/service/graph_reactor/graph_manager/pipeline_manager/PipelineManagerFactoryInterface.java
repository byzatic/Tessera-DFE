package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PipelineManagerFactoryInterface {
    PipelineManagerInterface getNewPipelineManager(GraphNodeRef currentExecutionNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef) throws OperationIncompleteException;

    PipelineManagerInterface getNewPipelineManager(GraphNodeRef currentExecutionNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, @NotNull ImmediateSchedulerInterface scheduler, JobEventListener... listeners) throws OperationIncompleteException;
}
