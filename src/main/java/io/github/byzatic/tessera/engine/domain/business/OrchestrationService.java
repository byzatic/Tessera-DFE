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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OrchestrationService — версия на ImmediateScheduler.
 *
 * Поведение:
 *  - Стартует все инфраструктурные сервисы (ServicesManagerInterface).
 *  - Добавляет одну задачу в ImmediateScheduler: выполнение графа (GraphManagerInterface).
 *  - Ожидает завершения задачи (complete/failed/timeout/cancelled) через JobEventListener.
 *  - По завершении/ошибке останавливает сервисы и закрывает шедулер.
 *
 * Отличия от прежней реализации:
 *  - Нет busy-wait; синхронизация через CountDownLatch и события шедулера.
 *  - Поддержка мягкой остановки через CancellationToken (если задача её поддерживает).
 */
public final class OrchestrationService implements OrchestrationServiceInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);

    public enum ServiceState { STARTING, RUNNING, STOPPING, STOPPED, FAULT }

    private final ServicesManagerInterface serviceManager;
    private final GraphManagerInterface graphManager;
    private final ImmediateSchedulerInterface scheduler;
    private final Duration stopGrace;

    private volatile ServiceState state = ServiceState.STARTING;
    private volatile UUID jobId;

    public OrchestrationService(@NotNull ServicesManagerInterface serviceManager,
                                @NotNull GraphManagerInterface graphManager) {
        this(serviceManager, graphManager,
                new ImmediateScheduler.Builder()
                        .defaultGrace(Duration.ofSeconds(10))
                        .build(),
                Duration.ofSeconds(10));
    }

    public OrchestrationService(@NotNull ServicesManagerInterface serviceManager,
                                @NotNull GraphManagerInterface graphManager,
                                @NotNull ImmediateSchedulerInterface scheduler,
                                @NotNull Duration stopGrace) {
        this.serviceManager = serviceManager;
        this.graphManager = graphManager;
        this.scheduler = scheduler;
        this.stopGrace = stopGrace;
    }

    @Override
    public void start() throws BusinessLogicException {
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Throwable> terminalError = new AtomicReference<>(null);

        // Подписываемся на события задач.
        JobEventListener listener = new JobEventListener() {
            @Override
            public void onStart(UUID jobId) {
                logger.debug("Graph job {} started", jobId);
            }

            @Override
            public void onComplete(UUID jobId) {
                logger.debug("Graph job {} completed", jobId);
                finished.countDown();
            }

            @Override
            public void onError(UUID jobId, Throwable error) {
                logger.error("Graph job {} failed", jobId, error);
                terminalError.set(error);
                finished.countDown();
            }

            @Override
            public void onTimeout(UUID jobId) {
                logger.error("Graph job {} timed out", jobId);
                finished.countDown();
            }

            @Override
            public void onCancelled(UUID jobId) {
                logger.warn("Graph job {} cancelled", jobId);
                finished.countDown();
            }
        };

        scheduler.addListener(listener);

        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.info("OrchestrationService starting...");

            state = ServiceState.STARTING;
            serviceManager.runAllServices();

            // Планируем единственную доменную задачу — расчёт графа.
            Task graphTask = new GraphManagerTask(graphManager);
            jobId = scheduler.addTask(graphTask);

            state = ServiceState.RUNNING;

            // Блокируемся до окончания задачи (любое терминальное событие).
            finished.await();

            // Доп. диагностика состояния
            if (jobId != null) {
                JobInfo info = scheduler.query(jobId).orElse(null);
                logger.info("Graph job terminal state: {}", info);
                if (info != null && info.state == JobState.FAILED && terminalError.get() == null) {
                    terminalError.set(new RuntimeException(info.lastError));
                }
            }

            // Если ошибка — пробрасываем как бизнес-исключение.
            if (terminalError.get() != null) {
                state = ServiceState.FAULT;
                throw new BusinessLogicException("Graph job failed", terminalError.get());
            }

            // Нормальное завершение
            state = ServiceState.STOPPING;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Orchestration interrupted", ie);
        } catch (BusinessLogicException e) {
            // уже типизировано
            throw e;
        } catch (Throwable t) {
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Orchestration fatal error: " + t.getMessage(), t);
        } finally {
            // Гарантированная остановка сервисов и шедулера
            try {
                if (jobId != null) {
                    scheduler.stopTask(jobId, stopGrace);
                    scheduler.removeTask(jobId, stopGrace);
                }
            } catch (Throwable t) {
                logger.warn("Error while stopping graph job: {}", t.toString());
            }

            try {
                scheduler.close();
            } catch (Exception e) {
                logger.warn("Error while closing scheduler", e);
            }

            try {
                serviceManager.stopAllServices();
            } catch (Throwable t) {
                logger.warn("Error while stopping services", t);
            }

            if (state != ServiceState.FAULT) {
                state = ServiceState.STOPPED;
            }

            logger.info("OrchestrationService stopped with state={}", state);
            scheduler.removeListener(listener);
        }
    }

    @Override
    public void close() {
        try {
            if (jobId != null) {
                scheduler.stopTask(jobId, stopGrace);
                scheduler.removeTask(jobId, stopGrace);
            }
        } catch (Throwable ignored) {
        }
        try {
            scheduler.close();
        } catch (Exception ignored) {
        }
        try {
            serviceManager.stopAllServices();
        } catch (Throwable ignored) {
        }
        state = ServiceState.STOPPED;
    }

    public ServiceState state() {
        return state;
    }

    /**
     * Адаптер GraphManagerInterface -> Task (ImmediateScheduler).
     * Если GraphManager умеет корректно реагировать на прерывания, остановка пройдёт мягко.
     */
    private static final class GraphManagerTask implements Task {
        private final GraphManagerInterface graphManager;

        private GraphManagerTask(GraphManagerInterface graphManager) {
            this.graphManager = graphManager;
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            token.throwIfStopRequested(); // быстрый отказ, если уже попросили остановку
            try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
                graphManager.runGraph(); // доменная работа
            }
            token.throwIfStopRequested(); // финальная точка кооперативной остановки
        }

        @Override
        public void onStopRequested() {
            // Если GraphManagerInterface поддерживает кооперативную остановку — вызовите здесь.
            // Пример:
            // if (graphManager instanceof SupportsStop s) { s.requestStop(); }
        }
    }
}
