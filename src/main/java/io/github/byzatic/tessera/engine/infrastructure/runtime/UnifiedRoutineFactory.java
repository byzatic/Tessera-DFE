package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.lib.configio.unified.RoutineCreationRequest;
import io.github.byzatic.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;

import java.util.Objects;

/**
 * Creates workflow routines through the unified project runtime.
 *
 * <p>The owning {@code ProjectRevisionHandle} controls the runtime-session lifecycle.</p>
 */
public final class UnifiedRoutineFactory {

    private final ProjectRuntimeSession runtimeSession;

    public UnifiedRoutineFactory(ProjectRuntimeSession runtimeSession) {
        this.runtimeSession = Objects.requireNonNull(runtimeSession, "runtimeSession");
    }

    public WorkflowRoutineInterface create(
            String routineName,
            MCg3WorkflowRoutineApiInterface routineApi,
            HealthFlagProxy health
    ) throws OperationIncompleteException {
        try {
            return runtimeSession.createRoutine(
                    RoutineCreationRequest.newBuilder()
                            .routineName(routineName)
                            .api(routineApi)
                            .health(health)
                            .build()
            );
        } catch (TesseraProjectException exception) {
            throw new OperationIncompleteException(
                    "Cannot create workflow routine " + routineName,
                    exception
            );
        }
    }
}
