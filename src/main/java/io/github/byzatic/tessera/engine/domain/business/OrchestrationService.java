package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.commons.schedulers.cron.CronScheduler;
import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.cron.CronTask;
import io.github.byzatic.commons.schedulers.immediate.*;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OrchestrationService с поддержкой ImmediateScheduler (сервисы) и CronScheduler (граф).
 *
 * Новая семантика:
 *  - serviceManager работает автономно (runAllServices).
 *  - graphManager.runGraph() вызывается ТОЛЬКО по cron (без обязательного первого немедленного прогона).
 *  - start() блокируется, пока не произойдёт фатальное событие в listener’ах (error/timeout) ИЛИ пока не будет вызван stop().
 *  - При фатале start() бросает BusinessLogicException. При stop() — возвращается нормально.
 */
public final class OrchestrationService implements OrchestrationServiceInterface, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(OrchestrationService.class);

    public enum ServiceState { STARTING, RUNNING, STOPPING, STOPPED, FAULT }

    private final ServicesManagerFactoryInterface servicesManagerFactory;
    private final GraphManagerFactoryInterface graphManagerFactory;

    private final ImmediateSchedulerInterface immediateScheduler;
    private final CronSchedulerInterface cronScheduler;

    private final Duration stopGrace;

    /** Cron выражение для графа. Если null/пусто — граф НЕ планируется. */
    private volatile @Nullable String graphCron;

    private volatile ServicesManagerInterface serviceManager;
    private volatile GraphManagerInterface graphManager;

    private volatile UUID cronGraphJobId;

    private volatile ServiceState state = ServiceState.STOPPED;

    /** Латч для блокировки start() до фатала или остановки. */
    private final CountDownLatch waitUntilStopOrFatal = new CountDownLatch(1);
    private final AtomicReference<Throwable> fatalError = new AtomicReference<>(null);

    /** Храним listener’ы, чтобы снять их при stop(). */
    private JobEventListener immediateListenerRef;
    private io.github.byzatic.commons.schedulers.cron.JobEventListener cronListenerRef;

    // ----- Конструкторы -----

    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerFactoryInterface graphManagerFactory,
                                @NotNull ImmediateSchedulerInterface immediateScheduler,
                                @NotNull CronSchedulerInterface cronScheduler,
                                @NotNull Duration stopGrace,
                                @Nullable String graphCron) {
        this.servicesManagerFactory = servicesManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
        this.immediateScheduler = immediateScheduler;
        this.cronScheduler = cronScheduler;
        this.stopGrace = stopGrace;
        this.graphCron = normalizeCron(graphCron);
    }

    public OrchestrationService(@NotNull ServicesManagerFactoryInterface servicesManagerFactory,
                                @NotNull GraphManagerFactoryInterface graphManagerFactory) {
        this(servicesManagerFactory,
                graphManagerFactory,
                new ImmediateScheduler.Builder().defaultGrace(Duration.ofSeconds(10)).build(),
                new CronScheduler.Builder().build(),
                Duration.ofSeconds(10),
                Configuration.CRON_EXPRESSION_STRING);
    }

    /** Позволяет задать/поменять cron-строку перед start(). */
    public void setGraphCron(@Nullable String cron) {
        this.graphCron = normalizeCron(cron);
    }

    private @Nullable String normalizeCron(@Nullable String cron) {
        return (cron != null && !cron.isBlank()) ? cron : null;
    }

    public ServiceState state() { return state; }

    private void configureImmediateScheduler() {
        // ---- 1) Listener’ы на ImmediateScheduler (ошибки сервисов и любых immediate-задач) ----
        immediateListenerRef = new JobEventListener() {
            private void fail(UUID jobId, String reason, Throwable error) {
                // Пытаемся извлечь lastError для детализации
                Throwable toStore = error;
                try {
                    Optional<JobInfo> oi = immediateScheduler.query(jobId);
                    if (oi.isPresent() && oi.get().lastError != null) {
                        toStore = new RuntimeException(oi.get().lastError, error);
                    }
                } catch (Throwable ignored) { /* no-op */ }

                if (fatalError.compareAndSet(null, (toStore != null ? toStore : new RuntimeException(reason)))) {
                    logger.error("Fatal from ImmediateScheduler: {} (jobId={})", reason, jobId, toStore);
                    waitUntilStopOrFatal.countDown();
                }
            }

            @Override public void onError(UUID jobId, Throwable error) { fail(jobId, "Immediate job error", error); }
            @Override public void onTimeout(UUID jobId)             { fail(jobId, "Immediate job timeout", null); }
            @Override public void onCancelled(UUID jobId)           { /* отмена не считается фаталом */ }
            @Override public void onComplete(UUID jobId)            { /* успех — просто лог */ }
        };
        immediateScheduler.addListener(immediateListenerRef);
    }

    private void configureCronScheduler() {
        cronListenerRef = new io.github.byzatic.commons.schedulers.cron.JobEventListener() {
            private void fail(UUID jobId, String reason, Throwable error) {
                if (fatalError.compareAndSet(null, (error != null ? error : new RuntimeException(reason)))) {
                    logger.error("Fatal from CronScheduler: {} (jobId={})", reason, jobId, error);
                    waitUntilStopOrFatal.countDown();
                }
            }
            @Override public void onError(UUID jobId, Throwable error) { fail(jobId, "Cron job error", error); }
            @Override public void onTimeout(UUID jobId)               { fail(jobId, "Cron job timeout", null); }
            @Override public void onCancelled(UUID jobId)             { /* не фатал */ }
            @Override public void onComplete(UUID jobId)              { /* успех — просто лог */ }
        };
        cronScheduler.addListener(cronListenerRef);
    }

    @Override
    public void start() throws BusinessLogicException {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.info("OrchestrationService starting...");
            state = ServiceState.STARTING;

            // ---- 1) Listener’ы на ImmediateScheduler (ошибки сервисов и любых immediate-задач) ----
            configureImmediateScheduler();

            // ---- 2) Запускаем сервисы (живут автономно) ----
            this.serviceManager = servicesManagerFactory.create(immediateScheduler);
            serviceManager.runAllServices();

            // ---- 3) Создаём graphManager и (если есть cron) планируем граф ----
            this.graphManager = graphManagerFactory.create(immediateScheduler);

            final String cron = this.graphCron;
            if (cron != null) {
                configureCronScheduler();

                CronTask graphCronTask = new CronTask() {
                    @Override
                    public void run(io.github.byzatic.commons.schedulers.cron.CancellationToken token) throws Exception {
                        token.throwIfStopRequested();
                        try (AutoCloseable ignored2 = Configuration.MDC_ENGINE_CONTEXT.use()) {
                            graphManager.runGraph();
                        }
                        token.throwIfStopRequested();
                    }
                    @Override
                    public void onStopRequested() {
                        // если появится кооперативная остановка графа — вызвать её здесь
                    }
                };

                // Планируем ПОВТОРЯЮЩУЮСЯ задачу по cron:
                // disallowOverlap=true (никаких пересечений), runImmediately=false (только по расписанию)
                this.cronGraphJobId = cronScheduler.addJob(cron, graphCronTask, true, true);
                logger.info("Graph scheduled via CronScheduler: {} (jobId={})", cron, cronGraphJobId);
            } else {
                logger.warn("graphCron is not set — graphManager will NOT be scheduled.");
            }

            // ---- 4) Блокируемся до фатала или остановки ----
            state = ServiceState.RUNNING;
            logger.info("OrchestrationService is RUNNING. Waiting for stop() or fatal error...");
            waitUntilStopOrFatal.await();

            // Если пришёл фатал — бросаем исключение
            Throwable fatal = fatalError.get();
            if (fatal != null) {
                state = ServiceState.FAULT;
                throw new BusinessLogicException("Orchestration fatal error", fatal);
            }

            // Иначе — это была штатная остановка
            logger.info("OrchestrationService stopped gracefully.");

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Interrupted while waiting in start()", ie);
        } catch (BusinessLogicException ble) {
            throw ble;
        } catch (Throwable t) {
            state = ServiceState.FAULT;
            throw new BusinessLogicException("Unexpected orchestration error", t);
        }
    }

    /**
     * Корректная остановка: разблокируем start(), снимаем cron-задачу, останавливаем сервисы и закрываем планировщики.
     */
    public void stop() {
        logger.info("OrchestrationService stopping...");
        state = ServiceState.STOPPING;

        // Разблокируем start() (если ещё не разблокирован фаталом)
        waitUntilStopOrFatal.countDown();

        // Снимаем cron-задачу (если была)
        try {
            UUID jobId = this.cronGraphJobId;
            if (jobId != null) {
                try {
                    cronScheduler.removeJob(jobId, stopGrace);
                } catch (Throwable t) {
                    try { cronScheduler.stopJob(jobId, stopGrace); } catch (Throwable ignored) {}
                } finally {
                    this.cronGraphJobId = null;
                }
            }
        } catch (Throwable ignored) {}

        // Снимаем listener’ы
        try {
            if (cronListenerRef != null) {
                cronScheduler.removeListener(cronListenerRef);
                cronListenerRef = null;
            }
        } catch (Throwable ignored) {}
        try {
            if (immediateListenerRef != null) {
                immediateScheduler.removeListener(immediateListenerRef);
                immediateListenerRef = null;
            }
        } catch (Throwable ignored) {}

        // Останавливаем сервисы
        try {
            if (serviceManager != null) {
                serviceManager.stopAllServices();
            }
        } catch (Throwable ignored) {}

        // Закрываем шедуллеры
        try { cronScheduler.close(); } catch (Exception ignored) {}
        try { immediateScheduler.close(); } catch (Exception ignored) {}

        state = ServiceState.STOPPED;
        logger.info("OrchestrationService is STOPPED.");
    }

    @Override
    public void close() {
        stop();
    }
}