package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;

public interface ModuleLoaderInterface {
    public WorkflowRoutineInterface getModule(String workflowRoutineClassName, MCg3WorkflowRoutineApiInterface workflowRoutineApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException;
}
