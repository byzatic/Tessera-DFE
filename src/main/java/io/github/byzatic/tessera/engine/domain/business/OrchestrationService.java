package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;

import io.github.byzatic.commons.schedulers.immediate.CancellationToken;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.commons.schedulers.immediate.JobInfo;
import io.github.byzatic.commons.schedulers.immediate.JobState;
import io.github.byzatic.commons.schedulers.immediate.Task;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OrchestrationService — версия на ImmediateScheduler,
 * использующая ServicesManagerFactory и GraphManagerFactory.
 *
 * Поведение:
 *  1) Создаёт общий ImmediateScheduler.
 *  2) Через ServicesManagerFactory создаёт менеджер сервисов, передавая общий scheduler и listener; запускает сервисы.
 *  3) Через GraphManagerFactory создаёт GraphManager, передавая общий scheduler и listener.
 *  4) Планирует единственную задачу: graphManager.runGraph().
 *  5) Ожидает терминального события графовой задачи и корректно останавливается.
 */
public final class OrchestrationService implements OrchestrationServiceInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);

    public enum ServiceState { STARTING, RUNNING, STOPPING, STOPPED, FAULT }

    private final ServicesManagerFactoryInterface servicesManagerFactory;
    private final GraphManagerFactoryInterface graphManagerFactory;
    private final ImmediateSchedulerInterface scheduler;
    private final Duration stopGrace;

    private volatile ServicesManagerInterface serviceManager;
    private volatile GraphManagerInterface graphManager;

    private volatile ServiceState state = ServiceState.STARTING;
    private volatile UUID jobId;

    // Рекомендуемый конструктор: создаём свой scheduler и задаём дефолтный grace
    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerFactoryInterface graphManagerFactory) {
        this(servicesManagerFactory,
                graphManagerFactory,
                new ImmediateScheduler.Builder().defaultGrace(Duration.ofSeconds(10)).build(),
                Duration.ofSeconds(10));
    }

    // Перегрузка: внешний scheduler и настраиваемый stopGrace
    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerFactoryInterface graphManagerFactory,
                                @NotNull ImmediateSchedulerInterface scheduler,
                                @NotNull Duration stopGrace) {
        this.servicesManagerFactory = servicesManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.scheduler = scheduler;
        this.stopGrace = stopGrace;
    }

    @Override
    public void start() throws BusinessLogicException {
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Throwable> terminalError = new AtomicReference<>(null);

        // Единый listener для наблюдения за задачей графа и (опционально) за задачами сервисов.
        JobEventListener listener = new JobEventListener() {
            @Override
            public void onStart(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.debug("Graph job {} started", id);
                }
            }

            @Override
            public void onComplete(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.debug("Graph job {} completed", id);
                    finished.countDown();
                }
            }

            @Override
            public void onError(UUID id, Throwable error) {
                if (id != null && id.equals(jobId)) {
                    logger.error("Graph job {} failed", id, error);
                    terminalError.set(error);
                    finished.countDown();
                }
            }

            @Override
            public void onTimeout(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.error("Graph job {} timed out", id);
                    finished.countDown();
                }
            }

            @Override
            public void onCancelled(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.warn("Graph job {} cancelled", id);
                    finished.countDown();
                }
            }
        };

        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.info("OrchestrationService starting...");
            state = ServiceState.STARTING;

            // Создаём менеджер сервисов: общий scheduler + listener внутрь
//            this.serviceManager = servicesManagerFactory.create(scheduler, listener);
            this.serviceManager = servicesManagerFactory.create(listener);
            serviceManager.runAllServices();

            // Создаём GraphManager: общий scheduler + listener внутрь
//            this.graphManager = graphManagerFactory.create(scheduler, listener);
            this.graphManager = graphManagerFactory.create(listener);

            // Планируем единственную доменную задачу — выполнение графа
            Task graphTask = new GraphManagerTask(graphManager);
            jobId = scheduler.addTask(graphTask);

            state = ServiceState.RUNNING;

            // Ожидаем терминальное событие
            finished.await();

            // Диагностика финального состояния
            if (jobId != null) {
                JobInfo info = scheduler.query(jobId).orElse(null);
                logger.info("Graph job terminal state: {}", info);
                if (info != null && info.state == JobState.FAILED && terminalError.get() == null && info.lastError != null) {
                    terminalError.set(new RuntimeException(info.lastError));
                }
            }

            if (terminalError.get() != null) {
                state = ServiceState.FAULT;
                throw new BusinessLogicException("Graph job failed", terminalError.get());
            }

            state = ServiceState.STOPPING;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Orchestration interrupted", ie);
        } catch (BusinessLogicException e) {
            throw e;
        } catch (Throwable t) {
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Orchestration fatal error: " + t.getMessage(), t);
        } finally {
            // Остановка графа и сервисов + закрытие шедуллера
            try {
                if (jobId != null) {
                    scheduler.stopTask(jobId, stopGrace);
                    scheduler.removeTask(jobId, stopGrace);
                }
            } catch (Throwable t) {
                logger.warn("Error while stopping graph job: {}", t.toString());
            }

            try {
                if (serviceManager != null) {
                    serviceManager.stopAllServices();
                }
            } catch (Throwable t) {
                logger.warn("Error while stopping services", t);
            }

            try {
                scheduler.close();
            } catch (Exception e) {
                logger.warn("Error while closing scheduler", e);
            }

            if (state != ServiceState.FAULT) {
                state = ServiceState.STOPPED;
            }

            logger.info("OrchestrationService stopped with state={}", state);
        }
    }

    @Override
    public void close() {
        try {
            if (jobId != null) {
                scheduler.stopTask(jobId, stopGrace);
                scheduler.removeTask(jobId, stopGrace);
            }
        } catch (Throwable ignored) {}
        try {
            if (serviceManager != null) {
                serviceManager.stopAllServices();
            }
        } catch (Throwable ignored) {}
        try {
            scheduler.close();
        } catch (Exception ignored) {}
        state = ServiceState.STOPPED;
    }

    public ServiceState state() {
        return state;
    }

    /** Адаптер: GraphManagerInterface -> ImmediateScheduler.Task */
    private static final class GraphManagerTask implements Task {
        private final GraphManagerInterface graphManager;

        private GraphManagerTask(GraphManagerInterface graphManager) {
            this.graphManager = graphManager;
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            token.throwIfStopRequested();
            try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
                graphManager.runGraph();
            }
            token.throwIfStopRequested();
        }

        @Override
        public void onStopRequested() {
            // Если появится кооперативная остановка графа — вызвать здесь.
            // Например:
            // if (graphManager instanceof SupportsStop s) s.requestStop();
        }
    }
}