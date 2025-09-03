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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OrchestrationService на ImmediateScheduler с корректной обработкой ошибок ServicesManager.
 *
 * Поведение:
 *  1) Вешает собственный JobEventListener на общий scheduler ДО запуска сервисов.
 *     Пока bootstrapPhase=true — любой фейл задачи считается фатальным (ошибка в сервисах).
 *  2) Создаёт ServicesManager через фабрику и запускает сервисы.
 *     Если listener уже зафиксировал ошибку сервиса — прерываем старт.
 *  3) Создаёт GraphManager через фабрику и планирует единственную задачу graphManager.runGraph().
 *     После планирования графа bootstrapPhase=false — далее слушатель реагирует только на графовую джобу.
 *  4) Дожидается терминального события графа или раннего фатального события с сервисов.
 *  5) Корректно останавливает граф, сервисы и закрывает scheduler.
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

    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerFactoryInterface graphManagerFactory) {
        this(servicesManagerFactory,
                graphManagerFactory,
                new ImmediateScheduler.Builder().defaultGrace(Duration.ofSeconds(10)).build(),
                Duration.ofSeconds(10));
    }

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
        final AtomicBoolean bootstrapPhase = new AtomicBoolean(true); // до планирования графа
        final AtomicBoolean listenerRegistered = new AtomicBoolean(false);

        // СВОЙ listener навешиваем напрямую на scheduler (не полагаемся, что фабрики его добавят)
        JobEventListener listener = new JobEventListener() {
            private void failEarlyIfBootstrap(UUID id, String reason, Throwable error) {
                if (bootstrapPhase.get()) {
                    // фиксируем ошибку сервиса на этапе бутстрапа
                    if (error != null) {
                        terminalError.compareAndSet(null, error);
                    } else {
                        // попытаемся достать текст ошибки из JobInfo
                        try {
                            Optional<JobInfo> oi = scheduler.query(id);
                            if (oi.isPresent() && oi.get().lastError != null) {
                                terminalError.compareAndSet(null, new RuntimeException(oi.get().lastError));
                            } else {
                                terminalError.compareAndSet(null, new RuntimeException(
                                        "Service task failed during bootstrap: " + id + " (" + reason + ")"));
                            }
                        } catch (Throwable t) {
                            terminalError.compareAndSet(null, new RuntimeException(
                                    "Service task failed during bootstrap: " + id + " (" + reason + ")", t));
                        }
                    }
                    finished.countDown();
                }
            }

            @Override public void onStart(UUID id) {
                // Ничего не делаем
            }

            @Override public void onComplete(UUID id) {
                // Завершение графа — только если это графовая джоба
                if (jobId != null && jobId.equals(id)) {
                    logger.debug("Graph job {} completed", id);
                    finished.countDown();
                }
            }

            @Override public void onError(UUID id, Throwable error) {
                if (jobId != null && jobId.equals(id)) {
                    logger.error("Graph job {} failed", id, error);
                    terminalError.compareAndSet(null, error);
                    finished.countDown();
                } else {
                    // Ошибка НЕ графовой джобы — во время бутстрапа считаем фатальной
                    failEarlyIfBootstrap(id, "error", error);
                }
            }

            @Override public void onTimeout(UUID id) {
                if (jobId != null && jobId.equals(id)) {
                    logger.error("Graph job {} timed out", id);
                    finished.countDown();
                } else {
                    failEarlyIfBootstrap(id, "timeout", null);
                }
            }

            @Override public void onCancelled(UUID id) {
                if (jobId != null && jobId.equals(id)) {
                    logger.warn("Graph job {} cancelled", id);
                    finished.countDown();
                } else {
                    failEarlyIfBootstrap(id, "cancelled", null);
                }
            }
        };

        // Регистрируем listener ДО старта сервисов
        scheduler.addListener(listener);
        listenerRegistered.set(true);

        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.info("OrchestrationService starting...");
            state = ServiceState.STARTING;

            // 1) Создаём и запускаем сервисы (используют Тот же scheduler).
            this.serviceManager = servicesManagerFactory.create(scheduler /* без лишних листнеров */);
            serviceManager.runAllServices();

            // Если уже прилетела ранняя ошибка (асинхронно из сервисов) — валим
            if (terminalError.get() != null) {
                state = ServiceState.FAULT;
                throw new BusinessLogicException("Service bootstrap failed", terminalError.get());
            }

            // 2) Создаём GraphManager (тот же scheduler), планируем его как единственную задачу.
            this.graphManager = graphManagerFactory.create(scheduler /* без лишних листнеров */);

            Task graphTask = new GraphManagerTask(graphManager);
            jobId = scheduler.addTask(graphTask);

            // Бутстрап завершён — дальше ошибки НЕ графовых задач пусть обрабатываются их владельцами
            bootstrapPhase.set(false);

            state = ServiceState.RUNNING;

            // 3) Ждём терминального события
            finished.await();

            // 4) Диагностика финального состояния графа
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
            // Снимаем listener (если успели навесить)
            if (listenerRegistered.get()) {
                try { scheduler.removeListener(listener); } catch (Throwable ignore) {}
            }

            // Останавливаем граф и сервисы + закрываем шедулер
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
                    logger.info("stopAllServices STARTED at {}", Instant.now());
                    serviceManager.stopAllServices();
                    logger.info("stopAllServices CANCELLED at {}", Instant.now());
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
        }
    }
}