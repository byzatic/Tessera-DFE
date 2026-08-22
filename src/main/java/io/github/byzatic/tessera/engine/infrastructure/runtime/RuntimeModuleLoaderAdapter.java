package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.domain.exception.PluginLoadingException;
import io.github.byzatic.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.lib.configio.unified.RoutineCreationRequest;
import io.github.byzatic.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;

import java.util.Objects;
import java.util.Set;

/**
 * Adapts the unified runtime session to the workflow routine loader used by the engine.
 */
final class RuntimeModuleLoaderAdapter implements ModuleLoaderInterface {

    private final ProjectRuntimeSession runtimeSession;

    RuntimeModuleLoaderAdapter(ProjectRuntimeSession runtimeSession) {
        this.runtimeSession = Objects.requireNonNull(runtimeSession, "runtimeSession");
    }

    @Override
    public WorkflowRoutineInterface getModule(
            String workflowRoutineClassName,
            MCg3WorkflowRoutineApiInterface workflowRoutineApi,
            HealthFlagProxy healthFlagProxy
    ) throws PluginLoadingException {
        try {
            return runtimeSession.createRoutine(
                    RoutineCreationRequest.newBuilder()
                            .routineName(workflowRoutineClassName)
                            .api(workflowRoutineApi)
                            .health(healthFlagProxy)
                            .build()
            );
        } catch (TesseraProjectException exception) {
            throw new PluginLoadingException(
                    "Cannot create workflow routine " + workflowRoutineClassName,
                    exception
            );
        }
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return runtimeSession.getAvailableRoutineNames();
    }

    @Override
    public void close() {
        // The project revision handle owns and closes the runtime session.
    }
}
