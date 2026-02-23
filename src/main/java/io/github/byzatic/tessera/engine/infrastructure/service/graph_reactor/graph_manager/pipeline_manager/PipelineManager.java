package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import io.github.byzatic.commons.schedulers.immediate.*;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.*;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.observability.PrometheusMetricsAgent;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager.PathManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.MCg3WorkflowRoutineApi;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.StorageApi;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContextFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader.ModuleLoaderInterface;
import io.github.byzatic.tessera.workflowroutine.configuration.ConfigurationParameter;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PipelineManager на ImmediateScheduler.
 *
 * Цели реализации:
 * - Параллельно запускать workers внутри стадии.
 * - Ждать завершения всей стадии и валидировать результаты (JobState + HealthFlag).
 * - НЕ плодить listeners на shared scheduler (иначе деградация по времени на каждой итерации).
 *
 * Важное правило владения:
 * - Если scheduler внешний (shared) — PipelineManager не должен "засорять" его listeners’ами.
 *   Поэтому используется global hub: один listener на scheduler + map jobId -> callback.
 */
public class PipelineManager implements PipelineManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(PipelineManager.class);

    private final ImmediateSchedulerInterface scheduler;
    private final boolean ownsScheduler;

    private final GraphNodeRef graphNodeRef;
    private final List<GraphNodeRef> pathToCurrentExecutionNodeRef;
    private final StorageManagerInterface storageManager;
    private final ModuleLoaderInterface moduleLoader;
    private final SupportPathResolver pathResolver;
    private final ExecutionContextFactoryInterface executionContextFactory;
    private final FullProjectRepository fullProjectRepository;

    /**
     * Hub терминальных событий для КОНКРЕТНОГО scheduler.
     * Ставим listener один раз на scheduler, дальше просто регистрируем callbacks по jobId.
     *
     * Это убирает:
     * - рост количества listeners с итерациями,
     * - аллокации/копирования из-за CopyOnWriteArrayList.removeListener().
     */
    private static final class SchedulerTerminalHub {
        private final ConcurrentHashMap<UUID, Runnable> onTerminal = new ConcurrentHashMap<>();

        private final JobEventListener listener = new JobEventListener() {
            private void fire(UUID jobId) {
                if (jobId == null) return;
                Runnable r = onTerminal.remove(jobId);
                if (r != null) {
                    try { r.run(); } catch (Throwable ignore) {}
                }
            }

            @Override public void onStart(UUID jobId) {}
            @Override public void onComplete(UUID jobId) { fire(jobId); }
            @Override public void onError(UUID jobId, Throwable error) { fire(jobId); }
            @Override public void onTimeout(UUID jobId) { fire(jobId); }
            @Override public void onCancelled(UUID jobId) { fire(jobId); }
        };

        void register(UUID jobId, Runnable callback) {
            if (jobId == null || callback == null) return;
            onTerminal.put(jobId, callback);
        }

        void unregister(UUID jobId) {
            if (jobId == null) return;
            onTerminal.remove(jobId);
        }

        /**
         * Закрываем гонку: если job уже терминальный к моменту регистрации callback — "догоняем" вручную.
         */
        void maybeFireIfTerminal(ImmediateSchedulerInterface scheduler, UUID jobId) {
            if (jobId == null) return;
            Optional<JobInfo> info;
            try {
                info = scheduler.query(jobId);
            } catch (Throwable t) {
                return;
            }
            if (info.isEmpty()) return;

            JobState s = info.get().state;
            if (s == JobState.COMPLETED || s == JobState.FAILED || s == JobState.CANCELLED || s == JobState.TIMEOUT) {
                Runnable r = onTerminal.remove(jobId);
                if (r != null) {
                    try { r.run(); } catch (Throwable ignore) {}
                }
            }
        }
    }

    /**
     * Глобальная привязка hub’ов к scheduler’ам.
     * Нужна для shared scheduler: один listener на инстанс scheduler’а на весь runtime.
     */
    private static final ConcurrentHashMap<ImmediateSchedulerInterface, SchedulerTerminalHub> HUBS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<ImmediateSchedulerInterface, Boolean> HUB_INSTALLED = new ConcurrentHashMap<>();

    private static SchedulerTerminalHub hubFor(ImmediateSchedulerInterface scheduler) {
        SchedulerTerminalHub hub = HUBS.computeIfAbsent(scheduler, s -> new SchedulerTerminalHub());

        // install once per scheduler
        if (HUB_INSTALLED.putIfAbsent(scheduler, Boolean.TRUE) == null) {
            scheduler.addListener(hub.listener);
        }
        return hub;
    }

    // ===== Constructor with external scheduler (shared) =====
    public PipelineManager(GraphNodeRef graphNodeRef,
                           List<GraphNodeRef> pathToCurrentExecutionNodeRef,
                           FullProjectRepository fullProjectRepository,
                           ModuleLoaderInterface moduleLoader,
                           StorageManagerInterface storageManager,
                           PathManagerInterface pathManagerInterface,
                           ExecutionContextFactoryInterface executionContextFactory,
                           ImmediateSchedulerInterface scheduler,
                           JobEventListener... listeners) throws OperationIncompleteException {

        this.graphNodeRef = Objects.requireNonNull(graphNodeRef, "graphNodeRef");
        this.pathToCurrentExecutionNodeRef = Objects.requireNonNull(pathToCurrentExecutionNodeRef, "pathToCurrentExecutionNodeRef");
        this.fullProjectRepository = Objects.requireNonNull(fullProjectRepository, "fullProjectRepository");
        this.moduleLoader = Objects.requireNonNull(moduleLoader, "moduleLoader");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.executionContextFactory = Objects.requireNonNull(executionContextFactory, "executionContextFactory");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.ownsScheduler = false;

        try {
            Objects.requireNonNull(pathManagerInterface, "pathManagerInterface");
            this.pathResolver = new SupportPathResolver(
                    pathManagerInterface.getStoragePathByGraphNodeRef(graphNodeRef),
                    pathManagerInterface.getProjectGlobalStorage()
            );
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }

        // Внешние listeners — это ответственность оркестрации.
        // Если всё же прокидываешь сюда listeners — они добавятся на shared scheduler и тоже могут размножаться.
        if (listeners != null) {
            for (JobEventListener l : listeners) {
                if (l != null) this.scheduler.addListener(l);
            }
        }

        // Ключевое: ставим terminal hub listener ровно один раз на scheduler
        hubFor(this.scheduler);
    }

    // ===== Constructor with self-hosted scheduler =====
    public PipelineManager(GraphNodeRef graphNodeRef,
                           List<GraphNodeRef> pathToCurrentExecutionNodeRef,
                           FullProjectRepository fullProjectRepository,
                           ModuleLoaderInterface moduleLoader,
                           StorageManagerInterface storageManager,
                           PathManagerInterface pathManagerInterface,
                           ExecutionContextFactoryInterface executionContextFactory) throws OperationIncompleteException {

        this.graphNodeRef = Objects.requireNonNull(graphNodeRef, "graphNodeRef");
        this.pathToCurrentExecutionNodeRef = Objects.requireNonNull(pathToCurrentExecutionNodeRef, "pathToCurrentExecutionNodeRef");
        this.fullProjectRepository = Objects.requireNonNull(fullProjectRepository, "fullProjectRepository");
        this.moduleLoader = Objects.requireNonNull(moduleLoader, "moduleLoader");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.executionContextFactory = Objects.requireNonNull(executionContextFactory, "executionContextFactory");

        this.scheduler = new ImmediateScheduler.Builder()
                .defaultGrace(Duration.ofSeconds(10))
                .build();

        this.ownsScheduler = true;

        try {
            Objects.requireNonNull(pathManagerInterface, "pathManagerInterface");
            this.pathResolver = new SupportPathResolver(
                    pathManagerInterface.getStoragePathByGraphNodeRef(graphNodeRef),
                    pathManagerInterface.getProjectGlobalStorage()
            );
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }

        // Для self-hosted scheduler логика та же: hub listener ставится один раз
        hubFor(this.scheduler);
    }

    @Override
    public void runPipeline() throws OperationIncompleteException {
        logger.debug("Run Pipeline for node {}", fullProjectRepository.getNode(graphNodeRef).getName());

        final SchedulerTerminalHub hub = hubFor(this.scheduler);

        Long startMs = null;
        String nodeItemId = null;
        String nodeItemName = null;
        String nodePath = null;

        // node_path нужен, но строим его один раз на весь pipeline
        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            startMs = System.currentTimeMillis();

            NodeItem nodeItem = fullProjectRepository.getNode(graphNodeRef);
            nodeItemId = nodeItem.getId();
            nodeItemName = nodeItem.getName();

            int size = pathToCurrentExecutionNodeRef.size();
            StringBuilder sb = new StringBuilder(size * 24);
            sb.append("[ ");
            boolean first = true;
            for (GraphNodeRef ref : pathToCurrentExecutionNodeRef) {
                NodeItem node = fullProjectRepository.getNode(ref);
                if (!first) sb.append(" ] - [ ");
                sb.append(node.getName());
                first = false;
            }
            sb.append(" ]");
            nodePath = sb.toString();
        }

        NodePipeline pipeline = fullProjectRepository.getPipeline(graphNodeRef);

        List<StagesConsistencyItem> stagesConsistencyItemList = new ArrayList<>(pipeline.getStagesConsistency());
        stagesConsistencyItemList.sort(Comparator.comparingInt(StagesConsistencyItem::getPosition));

        Map<String, StagesDescriptionItem> stageMap = pipeline.getStagesDescription().stream()
                .collect(Collectors.toMap(StagesDescriptionItem::getStageId, Function.identity()));

        for (StagesConsistencyItem stageConsistency : stagesConsistencyItemList) {
            StagesDescriptionItem stage = stageMap.get(stageConsistency.getStageId());
            if (stage == null) {
                throw new OperationIncompleteException("StageId " + stageConsistency.getStageId() + " hasn't description");
            }

            List<WorkersDescriptionItem> workers = stage.getWorkersDescription();
            if (workers == null || workers.isEmpty()) {
                logger.info("Stage {} has no workers — skipping", stage.getStageId());
                continue;
            }

            final CountDownLatch stageFinished = new CountDownLatch(workers.size());
            final List<UUID> stageJobs = new ArrayList<>(workers.size());
            final Map<UUID, HealthFlagProxy> healthByJob = new ConcurrentHashMap<>();

            try {
                for (WorkersDescriptionItem worker : workers) {
                    String workerName = worker.getName();

                    List<ConfigurationParameter> cfg = new ArrayList<>();
                    List<ConfigurationFilesItem> cfgFiles = worker.getConfigurationFiles();
                    if (cfgFiles != null) {
                        for (ConfigurationFilesItem f : cfgFiles) {
                            cfg.add(ConfigurationParameter.newBuilder()
                                    .parameterKey(f.getDescription())
                                    .parameterValue(pathResolver.processTemplate(f.getConfigurationFileId()))
                                    .build());
                        }
                    }

                    HealthFlagProxy health = HealthFlagProxy.newBuilder().build();

                    WorkflowRoutineInterface routine = moduleLoader.getModule(
                            workerName,
                            MCg3WorkflowRoutineApi.newBuilder()
                                    .setStorageApi(new StorageApi(storageManager, graphNodeRef, fullProjectRepository))
                                    .setConfigurationParameters(cfg)
                                    .setExecutionContext(executionContextFactory.getExecutionContext(
                                            graphNodeRef, pathToCurrentExecutionNodeRef,
                                            stage, worker, stageConsistency
                                    ))
                                    .build(),
                            health
                    );

                    UUID jobId = scheduler.addTask(new WorkflowRoutineTask(routine));
                    stageJobs.add(jobId);
                    healthByJob.put(jobId, health);

                    // Регистрируем countDown на терминальное событие (снимется автоматически при fire)
                    hub.register(jobId, stageFinished::countDown);
                    hub.maybeFireIfTerminal(scheduler, jobId);

                    logger.info("Scheduled workflowRoutine worker={} stage={} jobId={}",
                            workerName, stage.getStageId(), jobId);
                }

                // Ждём окончания стадии
                stageFinished.await();

                // Проверка результатов стадии
                for (UUID jobId : stageJobs) {
                    JobInfo info = scheduler.query(jobId).orElse(null);
                    if (info == null) {
                        throw new OperationIncompleteException("Job " + jobId + " not found after stage completion");
                    }
                    if (info.state != JobState.COMPLETED) {
                        String err = (info.lastError != null)
                                ? info.lastError
                                : ("Job " + jobId + " ended with state " + info.state);
                        throw new OperationIncompleteException(err);
                    }

                    HealthFlagProxy h = healthByJob.get(jobId);
                    if (h == null || h.getHealthFlagState() != HealthFlagState.COMPLETE) {
                        throw new OperationIncompleteException("Job " + jobId + " is not in COMPLETE health state");
                    }
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new OperationIncompleteException("Interrupted while waiting stage " + stage.getStageId(), ie);
            } finally {
                // Best-effort cleanup:
                // - если callback не успел сняться (например, не пришло событие) — удаляем руками
                for (UUID jobId : stageJobs) {
                    hub.unregister(jobId);
                }
                // - чистим задачи из scheduler registry
                for (UUID jobId : stageJobs) {
                    try { scheduler.removeTask(jobId); } catch (Throwable ignore) {}
                }
            }
        }

        // Метрика длительности pipeline на ноду — один раз по завершению pipeline
        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME && startMs != null) {
            long dur = System.currentTimeMillis() - startMs;
            PrometheusMetricsAgent.getInstance()
                    .publishNodePipelineExecutionTime(dur, nodeItemId, nodeItemName, nodePath);
        }
    }

    public void close() {
        if (ownsScheduler) {
            try { scheduler.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Адаптер: WorkflowRoutineInterface -> ImmediateScheduler.Task
     */
    private static final class WorkflowRoutineTask implements Task {
        private final WorkflowRoutineInterface routine;

        private WorkflowRoutineTask(WorkflowRoutineInterface routine) {
            this.routine = Objects.requireNonNull(routine, "routine");
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            token.throwIfStopRequested();
            routine.run();
            token.throwIfStopRequested();
        }

        @Override
        public void onStopRequested() {
            // Если появится кооперативная остановка рутины — подключить здесь.
        }
    }
}