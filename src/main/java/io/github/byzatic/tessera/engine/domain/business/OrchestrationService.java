package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.commons.schedulers.immediate.CancellationToken;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.commons.schedulers.immediate.JobInfo;
import io.github.byzatic.commons.schedulers.immediate.JobState;
import io.github.byzatic.commons.schedulers.immediate.Task;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OrchestrationService — версия на ImmediateScheduler с интеграцией нового ServicesManager
 * (внутрь ServicesManager прокидывается внешний JobEventListener).
 *
 * Поведение:
 *  - Создаёт/принимает единый ImmediateScheduler.
 *  - Создаёт ServicesManager через фабрику, передавая в него тот же шедуллер и listener.
 *  - Стартует все сервисы и планирует доменную задачу (GraphManager) в тот же шедуллер.
 *  - Ждёт терминального события графовой задачи и выполняет аккуратную остановку.
 */
public final class OrchestrationService implements OrchestrationServiceInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);

    public enum ServiceState { STARTING, RUNNING, STOPPING, STOPPED, FAULT }

    private final GraphManagerInterface graphManager;
    private final ImmediateSchedulerInterface scheduler;
    private final Duration stopGrace;

    /** Используем фабрику, чтобы гарантированно передать listener в ServicesManager. */
    private final ServicesManagerFactoryInterface servicesManagerFactory;

    /** Созданный менеджер сервисов (после start()). */
    private volatile ServicesManagerInterface serviceManager;

    private volatile ServiceState state = ServiceState.STARTING;
    private volatile UUID jobId;

    // === Рекомендуемый конструктор: создаём свой scheduler и ServicesManager через фабрику ===
    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerInterface graphManager) {
        this(servicesManagerFactory,
                graphManager,
                new ImmediateScheduler.Builder().defaultGrace(Duration.ofSeconds(10)).build(),
                Duration.ofSeconds(10));
    }

    // === Перегрузка: внешний scheduler и настраиваемый stopGrace ===
    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerInterface graphManager,
                                @NotNull ImmediateSchedulerInterface scheduler,
                                @NotNull Duration stopGrace) {
        this.servicesManagerFactory = servicesManagerFactory;
        this.graphManager = graphManager;
        this.scheduler = scheduler;
        this.stopGrace = stopGrace;
    }

    @Override
    public void start() throws BusinessLogicException {
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Throwable> terminalError = new AtomicReference<>(null);

        // Единый listener для ЧИС и для сервисов (прокинем его внутрь ServicesManager).
        JobEventListener listener = new JobEventListener() {
            @Override
            public void onStart(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.debug("Graph job {} started", id);
                } else {
                    logger.debug("Service job {} started", id);
                }
            }

            @Override
            public void onComplete(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.debug("Graph job {} completed", id);
                    finished.countDown();
                } else {
                    logger.debug("Service job {} completed", id);
                }
            }

            @Override
            public void onError(UUID id, Throwable error) {
                if (id != null && id.equals(jobId)) {
                    logger.error("Graph job {} failed", id, error);
                    terminalError.set(error);
                    finished.countDown();
                } else {
                    logger.warn("Service job {} failed", id, error);
                }
            }

            @Override
            public void onTimeout(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.error("Graph job {} timed out", id);
                    finished.countDown();
                } else {
                    logger.warn("Service job {} timed out", id);
                }
            }

            @Override
            public void onCancelled(UUID id) {
                if (id != null && id.equals(jobId)) {
                    logger.warn("Graph job {} cancelled", id);
                    finished.countDown();
                } else {
                    logger.info("Service job {} cancelled", id);
                }
            }
        };

        // listener будет добавлен в шедуллер внутри ServicesManager (через фабрику).
        // Дополнительно добавлять его здесь НЕ нужно, т.к. ServicesManager использует тот же scheduler.

        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.info("OrchestrationService starting...");

            state = ServiceState.STARTING;

            // Создаём ServicesManager так, чтобы внутрь попал и общий scheduler, и listener.
            this.serviceManager = servicesManagerFactory.create(scheduler, listener);

            // Стартуем инфраструктурные сервисы (задачи пойдут в Тот же scheduler).
            serviceManager.runAllServices();

            // Планируем доменную задачу — выполнение графа — в тот же scheduler.
            Task graphTask = new GraphManagerTask(graphManager);
            jobId = scheduler.addTask(graphTask);

            state = ServiceState.RUNNING;

            // Ждём терминального события по графу.
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
            // listener удалять здесь не требуется: он добавлялся через ServicesManager и
            // вместе с закрытием scheduler фактически теряет смысл.
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

    /**
     * Адаптер GraphManagerInterface -> Task (ImmediateScheduler).
     */
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
            // При наличии кооперативной остановки графа — вызвать здесь.
            // Пример:
            // if (graphManager instanceof SupportsStop s) { s.requestStop(); }
        }
    }
}