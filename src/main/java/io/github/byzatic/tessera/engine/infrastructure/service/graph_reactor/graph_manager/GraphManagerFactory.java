package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager;

import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Классическая фабрика GraphManager, совместимая с OrchestrationService.
 * Держит все зависимости GraphManager и создаёт его, принимая общий scheduler и слушатели событий.
 */
public final class GraphManagerFactory implements GraphManagerFactoryInterface {

    private final GraphManagerNodeRepositoryInterface graphManagerNodeRepository;
    private final PipelineManagerFactoryInterface pipelineManagerFactory;

    public GraphManagerFactory(
            @NotNull GraphManagerNodeRepositoryInterface graphManagerNodeRepository,
            @NotNull PipelineManagerFactoryInterface pipelineManagerFactory
    ) {
        this.graphManagerNodeRepository = Objects.requireNonNull(graphManagerNodeRepository, "graphManagerNodeRepository");
        this.pipelineManagerFactory = Objects.requireNonNull(pipelineManagerFactory, "pipelineManagerFactory");
    }

    @Override
    public GraphManagerInterface create(JobEventListener... listeners) {
        // Используем конструктор GraphManager, создающий собственный ImmediateScheduler
        return new GraphManager(
                graphManagerNodeRepository,
                pipelineManagerFactory,
                listeners
        );
    }

    @Override
    public GraphManagerInterface create(@NotNull ImmediateSchedulerInterface scheduler, JobEventListener... listeners) {
        Objects.requireNonNull(scheduler, "scheduler");
        // Используем конструктор GraphManager с внешним шедуллером и листнерами
        return new GraphManager(
                graphManagerNodeRepository,
                pipelineManagerFactory,
                scheduler,
                listeners
        );
    }
}