package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import ru.byzatic.metrics_core.workflowroutines_lib.api_engine.MCg3WorkflowRoutineApiInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.workflowroutines.WorkflowRoutineInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.workflowroutines.health.HealthFlagProxy;

public interface ModuleLoaderInterface {
    public WorkflowRoutineInterface getModule(String workflowRoutineClassName, MCg3WorkflowRoutineApiInterface workflowRoutineApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException;
}
