package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager.PathManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContextFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader.ModuleLoaderInterface;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PipelineManagerFactory implements PipelineManagerFactoryInterface {
    private FullProjectRepository fullProjectRepository = null;
    private ExecutionContextFactoryInterface executionContextFactory = null;
    private ModuleLoaderInterface moduleLoader = null;
    private StorageManagerInterface storageManager = null;
    private PathManagerInterface pathManager = null;
    private Class<? extends PipelineManagerInterface> pipelineManagerClazz = null;

    public PipelineManagerFactory(Class<? extends PipelineManagerInterface> clazz) throws OperationIncompleteException {
        try {
            clazz.getConstructor();
        } catch (Exception e) {
            throw new OperationIncompleteException("Creating test objects (" + clazz.getSimpleName() + ") with non-default constructors is not supported.", e);
        }
        this.pipelineManagerClazz = clazz;
    }

    public PipelineManagerFactory(@NotNull FullProjectRepository fullProjectRepository, @NotNull ModuleLoaderInterface moduleLoader, @NotNull StorageManagerInterface storageManager, @NotNull PathManagerInterface pathManager, @NotNull ExecutionContextFactoryInterface executionContextFactory) {
        this.fullProjectRepository = fullProjectRepository;
        this.moduleLoader = moduleLoader;
        this.storageManager = storageManager;
        this.pathManager = pathManager;
        this.executionContextFactory = executionContextFactory;
    }

    @Override
    public synchronized PipelineManagerInterface getNewPipelineManager(GraphNodeRef currentExecutionNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef) throws OperationIncompleteException {
        PipelineManagerInterface pipelineManager;
        try {
            if (pipelineManagerClazz != null) {
                pipelineManager = pipelineManagerClazz.getDeclaredConstructor().newInstance();
            } else {
                pipelineManager = new PipelineManager(currentExecutionNodeRef, pathToCurrentExecutionNodeRef, fullProjectRepository, moduleLoader, storageManager, pathManager, executionContextFactory);
                return pipelineManager;
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
        return pipelineManager;
    }

    @Override
    public synchronized PipelineManagerInterface getNewPipelineManager(GraphNodeRef currentExecutionNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, @NotNull ImmediateSchedulerInterface scheduler, JobEventListener... listeners) throws OperationIncompleteException {
        PipelineManagerInterface pipelineManager;
        try {
            if (pipelineManagerClazz != null) {
                pipelineManager = pipelineManagerClazz.getDeclaredConstructor().newInstance();
            } else {
                pipelineManager = new PipelineManager(currentExecutionNodeRef, pathToCurrentExecutionNodeRef, fullProjectRepository, moduleLoader, storageManager, pathManager, executionContextFactory, scheduler, listeners);
                return pipelineManager;
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
        return pipelineManager;
    }


}
