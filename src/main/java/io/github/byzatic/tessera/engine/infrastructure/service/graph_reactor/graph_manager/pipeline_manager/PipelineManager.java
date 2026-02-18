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
 * PipelineManager rewritten to use ImmediateScheduler.
 * <p>
 * Semantics:
 * - For each stage (by position), all workers are scheduled in parallel.
 * - We wait until ALL workers in the current stage finish (complete/fail/timeout/cancel),
 * then verify health flags and proceed to the next stage.
 * - Unique IDs kept for diagnostics.
 * <p>
 * Construction modes:
 * (1) External scheduler injection (share with orchestration, attach listeners).
 * (2) Self-hosted scheduler (created internally via Builder).
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

    // ===== Constructor with external scheduler (preferred) =====
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
        // external listeners
        if (listeners != null) {
            for (JobEventListener l : listeners) {
                if (l != null) this.scheduler.addListener(l);
            }
        }
        // internal lightweight logger
        this.scheduler.addListener(new LoggingListener());
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
        this.scheduler = new ImmediateScheduler.Builder().defaultGrace(Duration.ofSeconds(10)).build();
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
        this.scheduler.addListener(new LoggingListener());
    }

    @Override
    public void runPipeline() throws OperationIncompleteException {
        logger.debug("Run Pipeline for node {}", fullProjectRepository.getNode(graphNodeRef).getName());

        Long start = null;
        String nodeItemId = null;
        String nodeItemUUID = null;
        String nodeItemName = null;
        String nodePath = null;
        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            start = System.currentTimeMillis();
            NodeItem nodeItem = fullProjectRepository.getNode(graphNodeRef);
            nodeItemId = nodeItem.getId();
            nodeItemUUID = nodeItem.getUUID();
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

        // 1) Sorted stages
        NodePipeline pipeline = fullProjectRepository.getPipeline(graphNodeRef);
        List<StagesConsistencyItem> stagesConsistencyItemList = new ArrayList<>(pipeline.getStagesConsistency());
        stagesConsistencyItemList.sort(Comparator.comparingInt(StagesConsistencyItem::getPosition));
        logger.debug("Sort stagesConsistencyItemList by Position complete");

        // 2) Map<StageId, StagesDescriptionItem>
        List<StagesDescriptionItem> stagesDescriptionItemList = pipeline.getStagesDescription();
        Map<String, StagesDescriptionItem> stageMap = stagesDescriptionItemList.stream()
                .collect(Collectors.toMap(StagesDescriptionItem::getStageId, Function.identity()));
        logger.debug("Prepare Map<StageId, StagesDescriptionItem> complete");

        // 3) Stage-by-stage execution
        for (StagesConsistencyItem stagesConsistencyItem : stagesConsistencyItemList) {
            if (!stageMap.containsKey(stagesConsistencyItem.getStageId())) {
                throw new OperationIncompleteException("StageId " + stagesConsistencyItem.getStageId() + " hasn't description");
            }
            StagesDescriptionItem stagesDescriptionItem = stageMap.get(stagesConsistencyItem.getStageId());
            List<WorkersDescriptionItem> workersDescriptionItemList = stagesDescriptionItem.getWorkersDescription();
            if (workersDescriptionItemList == null || workersDescriptionItemList.isEmpty()) {
                logger.info("Stage {} has no workers — skipping", stagesDescriptionItem.getStageId());
                continue;
            }

            final List<UUID> stageJobs = new ArrayList<>();
            final Map<UUID, HealthFlagProxy> healthByJob = new ConcurrentHashMap<>();
            final CountDownLatch stageFinished = new CountDownLatch(workersDescriptionItemList.size());
            final Set<UUID> stageJobSet = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

            // Listener на терминальные события ТОЛЬКО наших джоб стадии
            final JobEventListener stageListener = new JobEventListener() {
                private boolean isTerminal(UUID id) {
                    Optional<JobInfo> info = scheduler.query(id);
                    if (info.isEmpty()) return false;
                    JobState s = info.get().state;
                    return s == JobState.COMPLETED || s == JobState.FAILED || s == JobState.CANCELLED || s == JobState.TIMEOUT;
                }

                private void maybeCountDown(UUID id) {
                    if (id != null && stageJobSet.contains(id) && isTerminal(id)) {
                        stageFinished.countDown();
                    }
                }

                @Override
                public void onStart(UUID jobId) {
                }

                @Override
                public void onComplete(UUID jobId) {
                    maybeCountDown(jobId);
                }

                @Override
                public void onError(UUID jobId, Throwable error) {
                    maybeCountDown(jobId);
                }

                @Override
                public void onTimeout(UUID jobId) {
                    maybeCountDown(jobId);
                }

                @Override
                public void onCancelled(UUID jobId) {
                    maybeCountDown(jobId);
                }
            };
            scheduler.addListener(stageListener);

            try {
                // 3.1) enqueue all workers in the stage
                for (WorkersDescriptionItem workersDescriptionItem : workersDescriptionItemList) {
                    String workerName = workersDescriptionItem.getName();
                    String workerDescription = workersDescriptionItem.getDescription();
                    List<ConfigurationFilesItem> workerConfigurationFilesList = workersDescriptionItem.getConfigurationFiles(); // -> ConfigurationParameter

                    List<ConfigurationParameter> configurationParameterList = new ArrayList<>();
                    if (workerConfigurationFilesList != null) {
                        for (ConfigurationFilesItem configurationFilesItem : workerConfigurationFilesList) {
                            configurationParameterList.add(
                                    ConfigurationParameter.newBuilder()
                                            .parameterKey(configurationFilesItem.getDescription())
                                            .parameterValue(
                                                    pathResolver.processTemplate(configurationFilesItem.getConfigurationFileId())
                                            )
                                            .build()
                            );
                        }
                    }

                    String uniqueId =
                            stagesDescriptionItem.getStageId()
                                    + "-" + workerName
                                    + "-" + UUID.randomUUID().toString().replace("-", "");

                    HealthFlagProxy healthFlagProxy = HealthFlagProxy.newBuilder().build();

                    WorkflowRoutineInterface workflowRoutine = moduleLoader.getModule(
                            workerName,
                            MCg3WorkflowRoutineApi.newBuilder()
                                    .setStorageApi(new StorageApi(storageManager, graphNodeRef, fullProjectRepository))
                                    .setConfigurationParameters(configurationParameterList)
                                    .setExecutionContext(
                                            this.executionContextFactory.getExecutionContext(
                                                    graphNodeRef, pathToCurrentExecutionNodeRef, stagesDescriptionItem, workersDescriptionItem, stagesConsistencyItem
                                            )
                                    )
                                    .build(),
                            healthFlagProxy
                    );

                    // ImmediateScheduler: Task wrapper
                    Task task = new WorkflowRoutineTask(workflowRoutine);

                    UUID jobId = scheduler.addTask(task);
                    stageJobs.add(jobId);
                    stageJobSet.add(jobId);
                    healthByJob.put(jobId, healthFlagProxy);

                    logger.info("Scheduled workflowRoutine worker={} stage={} jobId={}",
                            workerName, stagesDescriptionItem.getStageId(), jobId);
                }

                // 3.2) wait barrier
                stageFinished.await();

                // 3.3) post-stage validation
                for (UUID jobId : stageJobs) {
                    JobInfo info = scheduler.query(jobId).orElse(null);
                    if (info == null) {
                        throw new OperationIncompleteException("Job " + jobId + " not found after stage completion");
                    }
                    if (info.state != JobState.COMPLETED) {
                        String err = (info.lastError != null) ? info.lastError
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
                throw new OperationIncompleteException("Interrupted while waiting stage " + stagesDescriptionItem.getStageId(), ie);
            } finally {
                // remove stage listener & cleanup tasks
                scheduler.removeListener(stageListener);
                for (UUID id : stageJobs) {
                    try {
                        scheduler.removeTask(id);
                    } catch (Throwable ignore) {
                    }
                }
                if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
                    long dur = System.currentTimeMillis() - start;
                    PrometheusMetricsAgent.getInstance()
                            .publishNodePipelineExecutionTime(dur, nodeItemId, nodeItemName, nodePath);
                }
            }
        }
    }

    /**
     * If manager owns scheduler, call to close resources explicitly (optional).
     */
    public void close() {
        if (ownsScheduler) {
            try {
                scheduler.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Adapter: WorkflowRoutineInterface -> ImmediateScheduler.Task
     */
    private static final class WorkflowRoutineTask implements Task {
        private final WorkflowRoutineInterface routine;

        private WorkflowRoutineTask(WorkflowRoutineInterface routine) {
            this.routine = Objects.requireNonNull(routine, "routine");
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            token.throwIfStopRequested();
            routine.run(); // доменная работа рутины
            token.throwIfStopRequested();
        }

        @Override
        public void onStopRequested() {
            // Если у рутины есть кооперативная остановка — вызвать здесь.
            // if (routine instanceof SupportsStop s) { s.requestStop(); }
        }
    }

    /**
     * Лёгкий встроенный логирующий слушатель (опционален).
     */
    private static final class LoggingListener implements JobEventListener {
        @Override
        public void onStart(UUID jobId) {
        }

        @Override
        public void onComplete(UUID jobId) {
        }

        @Override
        public void onError(UUID jobId, Throwable error) {
        }

        @Override
        public void onTimeout(UUID jobId) {
        }

        @Override
        public void onCancelled(UUID jobId) {
        }
    }
}