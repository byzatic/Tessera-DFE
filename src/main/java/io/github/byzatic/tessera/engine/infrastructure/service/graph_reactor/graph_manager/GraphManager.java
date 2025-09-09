package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager;

import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;

import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversal;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversalInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;

import io.github.byzatic.commons.schedulers.immediate.CancellationToken;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.commons.schedulers.immediate.JobInfo;
import io.github.byzatic.commons.schedulers.immediate.JobState;
import io.github.byzatic.commons.schedulers.immediate.Task;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * GraphManager с использованием ImmediateScheduler.
 *
 * Поведение не менялось:
 *  - Берём корневые узлы графа из GraphManagerNodeRepositoryInterface.
 *  - Для каждого корневого узла планируем задачу, которая делегирует обход в GraphTraversal.
 *  - Ждём завершения всех задач; если любая завершилась не COMPLETED — бросаем OperationIncompleteException.
 */
public class GraphManager implements GraphManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(GraphManager.class);

    private final PipelineManagerFactoryInterface pipelineManagerFactory;
    private final GraphManagerNodeRepositoryInterface graphManagerNodeRepository;

    private final GraphTraversalInterface graphTraversal;

    private final ImmediateSchedulerInterface scheduler;
    private final boolean ownsScheduler;
    private StorageManagerInterface storageManager;

    /** Конструктор с внешним шедуллером (рекомендуемый для совместного использования в оркестрации). */
    public GraphManager(@NotNull StorageManagerInterface storageManager,
                        @NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository,
                        @NotNull PipelineManagerFactoryInterface pipelineManagerFactory,
                        @NotNull ImmediateSchedulerInterface scheduler,
                        JobEventListener... listeners) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository,
                new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory,
                new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(scheduler,
                new IllegalArgumentException(ImmediateSchedulerInterface.class.getSimpleName() + " should be NotNull"));

        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;

        this.scheduler = scheduler;
        this.ownsScheduler = false;
        this.storageManager = storageManager;

        // как и раньше — один traversal на весь менеджер
        this.graphTraversal = new GraphTraversal(graphManagerNodeRepository, pipelineManagerFactory, scheduler);

        // внешние слушатели (например, бизнес-логика)
        if (listeners != null) {
            for (JobEventListener l : listeners) {
                if (l != null) this.scheduler.addListener(l);
            }
        }
        // лёгкий внутренний логер (опционально)
        this.scheduler.addListener(new SilentLoggingListener());
    }

    /** Конструктор, создающий собственный ImmediateScheduler. */
    public GraphManager(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository,
                        @NotNull PipelineManagerFactoryInterface pipelineManagerFactory,
                        JobEventListener... listeners) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository,
                new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory,
                new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));

        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;

        this.scheduler = new ImmediateScheduler.Builder()
                .defaultGrace(Duration.ofSeconds(10))
                .build();
        this.ownsScheduler = true;

        this.graphTraversal = new GraphTraversal(graphManagerNodeRepository, pipelineManagerFactory);

        if (listeners != null) {
            for (JobEventListener l : listeners) {
                if (l != null) this.scheduler.addListener(l);
            }
        }
        this.scheduler.addListener(new SilentLoggingListener());
    }

    @Override
    public void runGraph() throws OperationIncompleteException {
        try {
            // 1) Получаем список корневых узлов (как и раньше).
            final List<GraphNodeRef> rootRefs = graphManagerNodeRepository.getRootNodes();
            if (rootRefs == null || rootRefs.isEmpty()) {
                logger.info("No root graph nodes found — nothing to execute.");
                return;
            }

            // 2) Готовим барьер, id-список и временный stage-listener,
            //    который засечёт терминальные события наших задач.
            final List<UUID> jobIds = new ArrayList<>(rootRefs.size());
            final Set<UUID> jobIdSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
            final CountDownLatch barrier = new CountDownLatch(rootRefs.size());

            final JobEventListener stageListener = new JobEventListener() {
                private boolean isTerminal(UUID id) {
                    Optional<JobInfo> info = scheduler.query(id);
                    if (info.isEmpty()) return false;
                    JobState s = info.get().state;
                    return s == JobState.COMPLETED || s == JobState.FAILED || s == JobState.CANCELLED || s == JobState.TIMEOUT;
                }
                private void maybeCountDown(UUID id) {
                    if (id != null && jobIdSet.contains(id) && isTerminal(id)) {
                        barrier.countDown();
                    }
                }
                @Override public void onStart(UUID jobId) {}
                @Override public void onComplete(UUID jobId) { maybeCountDown(jobId); }
                @Override public void onError(UUID jobId, Throwable error) { maybeCountDown(jobId); }
                @Override public void onTimeout(UUID jobId) { maybeCountDown(jobId); }
                @Override public void onCancelled(UUID jobId) { maybeCountDown(jobId); }
            };
            scheduler.addListener(stageListener);

            try {
                // 3) Для каждого корневого узла — отдельная задача ImmediateScheduler,
                //    внутри которой выполняется прежний traversal.traverse(rootNode).
                for (GraphNodeRef ref : rootRefs) {
                    final Node rootNode = graphManagerNodeRepository.getNode(ref);

                    Task task = new RootTraversalTask(graphTraversal, rootNode);
                    UUID id = scheduler.addTask(task);

                    jobIds.add(id);
                    jobIdSet.add(id);

                    logger.info("Scheduled graph traversal for root={} jobId={}", ref, id);
                }

                // 4) Барьер — ждём завершения всех traversal-задач.
                barrier.await();

                // 5) Проверяем терминальные статусы (как раньше проверялся health).
                for (UUID id : jobIds) {
                    JobInfo info = scheduler.query(id).orElse(null);
                    if (info == null) {
                        throw new OperationIncompleteException("Graph job " + id + " not found after execution");
                    }
                    if (info.state != JobState.COMPLETED) {
                        String err = (info.lastError != null) ? info.lastError
                                : ("Graph job " + id + " ended with state " + info.state);
                        throw new OperationIncompleteException(err);
                    }
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new OperationIncompleteException("Interrupted while waiting graph execution", ie);
            } finally {
                // Снимаем stage-listener и убираем задачи из реестра шедуллера.
                scheduler.removeListener(stageListener);
                for (UUID id : jobIds) {
                    try { scheduler.removeTask(id); } catch (Throwable ignore) {}
                }
                clear();
            }
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (Throwable t) {
            throw new OperationIncompleteException(t);
        } finally {
            // Если шедуллер наш — закрываем ресурсы.
            if (ownsScheduler) {
                try { scheduler.close(); } catch (Exception ignored) {}
            }
            clear();
        }
    }

    private void clear() throws OperationIncompleteException {
        try {
            this.storageManager.cleanupNodeStorages();
            logger.debug("StorageManager cleanup complete");
            this.graphManagerNodeRepository.clearNodeStatuses();
            logger.debug("GraphManagerNodeRepository cleanup complete");
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e);
        }
    }

    /** Адаптер: делегирует в прежний GraphTraversal. */
    private static final class RootTraversalTask implements Task {
        private final GraphTraversalInterface traversal;
        private final Node root;

        private RootTraversalTask(GraphTraversalInterface traversal, Node root) {
            this.traversal = Objects.requireNonNull(traversal, "traversal");
            this.root = Objects.requireNonNull(root, "root");
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            token.throwIfStopRequested();
            traversal.traverse(root);
            token.throwIfStopRequested();
        }

        @Override
        public void onStopRequested() {
            traversal.cancel();
        }
    }

    /** Тихий внутренний логер событий ImmediateScheduler (опционально). */
    private static final class SilentLoggingListener implements JobEventListener {
        @Override public void onStart(UUID jobId) {}
        @Override public void onComplete(UUID jobId) {}
        @Override public void onError(UUID jobId, Throwable error) {}
        @Override public void onTimeout(UUID jobId) {}
        @Override public void onCancelled(UUID jobId) {}
    }
}