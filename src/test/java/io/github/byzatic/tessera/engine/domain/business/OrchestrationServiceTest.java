package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrchestrationServiceTest {

    @Test
    public void cancellationDuringStopIsNotReportedAsFatal() throws Exception {
        ImmediateSchedulerInterface immediateScheduler = mock(ImmediateSchedulerInterface.class);
        CronSchedulerInterface cronScheduler = mock(CronSchedulerInterface.class);
        ServicesManagerInterface servicesManager = mock(ServicesManagerInterface.class);
        GraphManagerInterface graphManager = mock(GraphManagerInterface.class);
        ServicesManagerFactoryInterface servicesFactory = mock(ServicesManagerFactoryInterface.class);
        GraphManagerFactoryInterface graphFactory = mock(GraphManagerFactoryInterface.class);
        when(servicesFactory.create(any(), any())).thenReturn(servicesManager);
        when(graphFactory.create(any(), any())).thenReturn(graphManager);

        OrchestrationService service = new OrchestrationService(
                servicesFactory,
                graphFactory,
                immediateScheduler,
                cronScheduler,
                Duration.ofSeconds(1L),
                null
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> start = executor.submit(() -> {
                try {
                    service.start();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            assertTrue(service.awaitStarted(Duration.ofSeconds(1L)));

            ArgumentCaptor<JobEventListener> listenerCaptor =
                    ArgumentCaptor.forClass(JobEventListener.class);
            verify(immediateScheduler).addListener(listenerCaptor.capture());
            JobEventListener listener = listenerCaptor.getValue();
            doAnswer(invocation -> {
                listener.onError(UUID.randomUUID(), new RuntimeException("Traversal cancelled"));
                return null;
            }).when(graphManager).stop();

            service.stop();
            get(start);

            assertEquals(OrchestrationService.ServiceState.STOPPED, service.state());
            InOrder shutdownOrder = inOrder(immediateScheduler, graphManager);
            shutdownOrder.verify(immediateScheduler).removeListener(listener);
            shutdownOrder.verify(graphManager).stop();
        } finally {
            executor.shutdownNow();
        }
    }

    private static void get(Future<?> future) throws Exception {
        try {
            future.get(1L, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }
}
