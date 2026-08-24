package io.github.byzatic.tessera.engine.infrastructure.service.service_manager;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.Task;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.model.project.ServiceItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.runtime.UnifiedServiceFactory;
import io.github.byzatic.tessera.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.tessera.lib.configio.unified.ServiceCreationRequest;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServicesManagerTest {

    @Test
    public void shouldStartDifferentServicesWhenTheirNamesHaveCollidingHashes() throws Exception {
        assertEquals("Test fixture must use a real String hash collision",
                "Aa".hashCode(), "BB".hashCode());
        FullProjectRepository repository = mock(FullProjectRepository.class);
        when(repository.getGlobal()).thenReturn(ProjectGlobal.newBuilder()
                .storages(List.of())
                .services(List.of(service("Aa"), service("BB")))
                .build());

        ProjectRuntimeSession runtimeSession = mock(ProjectRuntimeSession.class);
        when(runtimeSession.createService(any(ServiceCreationRequest.class)))
                .thenReturn(mock(ServiceInterface.class), mock(ServiceInterface.class));

        ImmediateSchedulerInterface scheduler = mock(ImmediateSchedulerInterface.class);
        when(scheduler.addTask(any(Task.class)))
                .thenReturn(UUID.randomUUID(), UUID.randomUUID());

        ServicesManager manager = new ServicesManager(
                repository,
                new UnifiedServiceFactory(runtimeSession),
                mock(StorageManagerInterface.class),
                scheduler
        );

        manager.runAllServices();

        ArgumentCaptor<ServiceCreationRequest> requests =
                ArgumentCaptor.forClass(ServiceCreationRequest.class);
        verify(runtimeSession, times(2)).createService(requests.capture());
        assertEquals(List.of("Aa", "BB"), requests.getAllValues().stream()
                .map(ServiceCreationRequest::getServiceName)
                .toList());
        verify(scheduler, times(2)).addTask(any(Task.class));
    }

    private static ServiceItem service(String id) {
        return ServiceItem.newBuilder()
                .idName(id)
                .description(id)
                .options(List.of())
                .build();
    }
}
