package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public class GraphTraversal implements GraphTraversalInterface {
    private static final Logger logger = LoggerFactory.getLogger(GraphTraversal.class);

    private ImmediateSchedulerInterface immediateScheduler = null;
    private JobEventListener[] listeners = null;
    private PipelineManagerFactoryInterface pipelineManagerFactory = null;
    private GraphManagerNodeRepositoryInterface graphManagerNodeRepository = null;

    // ---- Cancellation support ----
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    /**
     * Request traversal cancellation. Thread-safe.
     */
    @Override
    public void cancel() {
        cancelRequested.set(true);
    }

    private boolean shouldCancel() {
        return cancelRequested.get() || Thread.currentThread().isInterrupted();
    }

    private void throwIfCancelled() throws OperationIncompleteException {
        if (shouldCancel()) {
            logger.warn("Graph traversal cancelled.");
            throw new OperationIncompleteException(new CancellationException("Traversal cancelled"));
        }
    }
    // --------------------------------

    public GraphTraversal(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository,
                          PipelineManagerFactoryInterface pipelineManagerFactory) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository, new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory, new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));
        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;
    }

    public GraphTraversal(@NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository,
                          PipelineManagerFactoryInterface pipelineManagerFactory,
                          ImmediateSchedulerInterface immediateScheduler,
                          JobEventListener... listeners) {
        ObjectsUtils.requireNonNull(graphManagerNodeRepository, new IllegalArgumentException(GraphManagerNodeRepositoryInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(pipelineManagerFactory, new IllegalArgumentException(PipelineManagerFactoryInterface.class.getSimpleName() + " should be NotNull"));
        this.graphManagerNodeRepository = graphManagerNodeRepository;
        this.pipelineManagerFactory = pipelineManagerFactory;
        this.immediateScheduler = immediateScheduler;
        this.listeners = listeners;
    }

    @Override
    public void traverse(@NotNull Node root) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(root, new IllegalArgumentException(Node.class.getSimpleName() + " should be NotNull"));
            Deque<NodePathState> stack = new ArrayDeque<>();
            stack.push(new NodePathState(root, new ArrayList<>()));
            logger.debug("root node pushed to stack");

            while (!stack.isEmpty()) {
                throwIfCancelled();

                NodePathState state = stack.peek();
                Node current = state.node;
                List<Node> currentPath = state.pathSoFar;

                switch (current.getNodeLifecycleState()) {
                    case NOTSTATED: // сохранил оригинальное состояние/опечатку
                        current.setNodeLifecycleState(NodeLifecycleState.WAITING);
                        List<Node> downstream = getDownstreamNodes(current);
                        for (Node child : downstream) {
                            List<Node> newPath = new ArrayList<>(currentPath);
                            newPath.add(current);
                            stack.push(new NodePathState(child, newPath));
                        }
                        break;

                    case WAITING:
                        // Неблокирующее ожидание готовности всех потомков с проверкой отмены
                        awaitChildrenOrCancel(current);
                        // Все дети готовы — обрабатываем текущий узел
                        stack.pop();
                        List<Node> fullPath = new ArrayList<>(currentPath);
                        fullPath.add(current);
                        throwIfCancelled(); // финальная проверка перед запуском пайплайна
                        processWithPath(current, fullPath);
                        current.setNodeLifecycleState(NodeLifecycleState.READY);
                        break;

                    case READY:
                        stack.pop();
                        break;
                }
            }
        } catch (OperationIncompleteException e) {
            // уже корректно обернуто/помечено как отмена или иная причина
            throw e;
        } catch (Exception e) {
            // Любая иная ошибка — оборачиваем как незавершенную операцию
            throw new OperationIncompleteException(e);
        }
    }

    /**
     * Периодически проверяет состояние потомков current до готовности всех или пока не будет запрошена отмена.
     * Избегает блокирующего вызова child.waitUntilReady(), чтобы поддержать прерывание.
     */
    private void awaitChildrenOrCancel(@NotNull Node current) throws OperationIncompleteException {
        final long spinSleepMillis = 10; // короткая пауза, чтобы не грузить CPU
        while (true) {
            throwIfCancelled();

            boolean allChildrenReady = true;
            for (Node child : getDownstreamNodes(current)) {
                if (child.getNodeLifecycleState() != NodeLifecycleState.READY) {
                    allChildrenReady = false;
                    break;
                }
            }

            if (allChildrenReady) {
                return;
            }

            // Короткая пауза + уважение к interrupt
            if (Thread.interrupted()) {
                // восстанавливаем семантику отмены
                Thread.currentThread().interrupt();
                throwIfCancelled();
            }
            // Неблокирующая пауза
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(spinSleepMillis));
        }
    }

    private List<Node> getDownstreamNodes(Node current) throws OperationIncompleteException {
        return graphManagerNodeRepository.getNodeDownstream(current);
    }

    private void processWithPath(Node current, List<Node> path) throws OperationIncompleteException {
        try {
            throwIfCancelled();
            logger.debug("Processing node: {}, path: {}", current,
                    path.stream().map(Node::toString).collect(Collectors.joining(" -> ")));
            if (immediateScheduler != null) {
                pipelineManagerFactory
                        .getNewPipelineManager(current.getGraphNodeRef(), convertPathToRefs(path), immediateScheduler, listeners)
                        .runPipeline();
            } else {
                pipelineManagerFactory
                        .getNewPipelineManager(current.getGraphNodeRef(), convertPathToRefs(path))
                        .runPipeline();
            }
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new OperationIncompleteException(e);
        }
    }

    private List<GraphNodeRef> convertPathToRefs(List<Node> path) {
        return path.stream()
                .map(Node::getGraphNodeRef)
                .collect(Collectors.toList());
    }

    // Вспомогательная структура, как и раньше
    private static final class NodePathState {
        final Node node;
        final List<Node> pathSoFar;
        NodePathState(Node node, List<Node> pathSoFar) {
            this.node = node;
            this.pathSoFar = pathSoFar;
        }
    }
}