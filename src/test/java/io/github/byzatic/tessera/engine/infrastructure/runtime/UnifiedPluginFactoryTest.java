package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.tessera.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.tessera.lib.configio.unified.RoutineCreationRequest;
import io.github.byzatic.tessera.lib.configio.unified.ServiceCreationRequest;
import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UnifiedPluginFactoryTest {

    @Test
    public void shouldCreateRoutineThroughUnifiedRuntime() throws Exception {
        ProjectRuntimeSession runtimeSession = mock(ProjectRuntimeSession.class);
        MCg3WorkflowRoutineApiInterface api = mock(MCg3WorkflowRoutineApiInterface.class);
        io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy health =
                io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy
                        .newBuilder()
                        .build();
        WorkflowRoutineInterface expected = mock(WorkflowRoutineInterface.class);
        when(runtimeSession.createRoutine(any(RoutineCreationRequest.class)))
                .thenReturn(expected);
        UnifiedRoutineFactory factory = new UnifiedRoutineFactory(runtimeSession);

        WorkflowRoutineInterface actual = factory.create("Routine", api, health);

        assertSame(expected, actual);
        ArgumentCaptor<RoutineCreationRequest> request =
                ArgumentCaptor.forClass(RoutineCreationRequest.class);
        verify(runtimeSession).createRoutine(request.capture());
        assertSame(api, request.getValue().getApi());
        assertSame(health, request.getValue().getHealth());
    }

    @Test
    public void shouldCreateServiceThroughUnifiedRuntime() throws Exception {
        ProjectRuntimeSession runtimeSession = mock(ProjectRuntimeSession.class);
        MCg3ServiceApiInterface api = mock(MCg3ServiceApiInterface.class);
        HealthFlagProxy health = HealthFlagProxy.newBuilder().build();
        ServiceInterface expected = mock(ServiceInterface.class);
        when(runtimeSession.createService(any(ServiceCreationRequest.class)))
                .thenReturn(expected);
        UnifiedServiceFactory factory = new UnifiedServiceFactory(runtimeSession);

        ServiceInterface actual = factory.create("Service", api, health);

        assertSame(expected, actual);
        ArgumentCaptor<ServiceCreationRequest> request =
                ArgumentCaptor.forClass(ServiceCreationRequest.class);
        verify(runtimeSession).createService(request.capture());
        assertSame(api, request.getValue().getApi());
        assertSame(health, request.getValue().getHealth());
    }
}
