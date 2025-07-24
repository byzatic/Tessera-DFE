package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesConsistencyItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.WorkersDescriptionItem;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.ExecutionContextInterface;

import java.util.List;

public interface ExecutionContextFactoryInterface {
    ExecutionContextInterface getExecutionContext(GraphNodeRef graphNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, StagesDescriptionItem stagesDescriptionItem, WorkersDescriptionItem workersDescriptionItem, StagesConsistencyItem stagesConsistencyItem) throws OperationIncompleteException;

    void reload();
}
