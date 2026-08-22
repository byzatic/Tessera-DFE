package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.lib.configio.unified.ProjectRevisionHandle;
import io.github.byzatic.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntime;
import io.github.byzatic.tessera.engine.application.runtime.ProjectRuntimeFactory;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationService;
import io.github.byzatic.tessera.engine.domain.repository.ProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_controller.StructureController;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectRepositoryImpl;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.StorageManager;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.GraphManagerFactory;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management.GraphPathManager;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management.GraphPathManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager.PathManager;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager.PathManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepository;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactory;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.PipelineManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContextFactory;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContextFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.ServicesManagerFactory;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Builds a complete per-revision dependency graph without static project singletons.
 */
public final class DefaultProjectRuntimeFactory implements ProjectRuntimeFactory {

    @Override
    public ProjectRuntime create(ProjectRevisionHandle revision)
            throws OperationIncompleteException {
        Objects.requireNonNull(revision, "revision");

        try {
            Path projectDirectory = revision.getProjectDirectory();
            ProjectRuntimeSession runtimeSession = revision.openRuntime();
            ProjectRepository repository = createProjectRepository(runtimeSession);
            ServiceLoaderInterface serviceLoader = createServiceLoader(runtimeSession);
            ModuleLoaderInterface moduleLoader = createModuleLoader(runtimeSession);

            StorageManagerInterface storageManager = createStorageManager(repository);
            PipelineManagerFactoryInterface pipelineManagerFactory = createPipelineManagerFactory(
                    repository,
                    moduleLoader,
                    storageManager,
                    projectDirectory
            );
            GraphManagerFactory graphManagerFactory = createGraphManagerFactory(
                    repository,
                    storageManager,
                    pipelineManagerFactory
            );
            ServicesManagerFactory servicesManagerFactory = createServicesManagerFactory(
                    repository,
                    serviceLoader,
                    storageManager
            );
            OrchestrationService orchestrationService = createOrchestrationService(
                    servicesManagerFactory,
                    graphManagerFactory
            );

            return createProjectRuntime(
                    revision.getRevisionId(),
                    orchestrationService,
                    storageManager,
                    moduleLoader,
                    serviceLoader
            );
        } catch (Exception exception) {
            throw new OperationIncompleteException(
                    "Cannot create runtime for revision " + revision.getRevisionId(),
                    exception
            );
        }
    }

    /**
     * Creates the repository backed by the project data already loaded for the revision.
     *
     * @param runtimeSession prepared project runtime session
     * @return repository for the revision
     * @throws OperationIncompleteException when the loaded project cannot be mapped
     */
    private ProjectRepository createProjectRepository(ProjectRuntimeSession runtimeSession)
            throws OperationIncompleteException {
        return new ProjectRepositoryImpl(runtimeSession.getProject());
    }

    /**
     * Creates a compatibility adapter for project service factories.
     *
     * @param runtimeSession project-scoped runtime resources
     * @return service loader adapter owned by the revision runtime session
     */
    private ServiceLoaderInterface createServiceLoader(ProjectRuntimeSession runtimeSession) {
        return new RuntimeServiceLoaderAdapter(runtimeSession);
    }

    /**
     * Creates a compatibility adapter for workflow routine factories.
     *
     * @param runtimeSession project-scoped runtime resources
     * @return module loader adapter owned by the revision runtime session
     */
    private ModuleLoaderInterface createModuleLoader(ProjectRuntimeSession runtimeSession) {
        return new RuntimeModuleLoaderAdapter(runtimeSession);
    }

    /**
     * Creates project-scoped storage state.
     *
     * @param repository project repository
     * @return storage manager for the runtime
     * @throws OperationIncompleteException when declared storages cannot be initialized
     */
    private StorageManagerInterface createStorageManager(ProjectRepository repository)
            throws OperationIncompleteException {
        return new StorageManager(repository);
    }

    /**
     * Creates the pipeline construction graph used by graph execution.
     *
     * @param repository project repository
     * @param moduleLoader workflow routine loader
     * @param storageManager project storage manager
     * @param projectDirectory staged project root directory
     * @return pipeline manager factory for the revision
     * @throws OperationIncompleteException when pipeline dependencies cannot be initialized
     */
    private PipelineManagerFactoryInterface createPipelineManagerFactory(
            ProjectRepository repository,
            ModuleLoaderInterface moduleLoader,
            StorageManagerInterface storageManager,
            Path projectDirectory
    ) throws OperationIncompleteException {
        StructureControllerInterface structureController =
                new StructureController(projectDirectory);
        PathManagerInterface pathManager = new PathManager(repository, structureController);
        GraphPathManagerInterface graphPathManager = new GraphPathManager(repository);
        ExecutionContextFactoryInterface executionContextFactory =
                new ExecutionContextFactory(repository, graphPathManager);
        return new PipelineManagerFactory(
                repository,
                moduleLoader,
                storageManager,
                pathManager,
                executionContextFactory
        );
    }

    /**
     * Creates the graph manager factory and its node repository.
     *
     * @param repository project repository
     * @param storageManager project storage manager
     * @param pipelineManagerFactory pipeline manager factory
     * @return graph manager factory for the revision
     * @throws OperationIncompleteException when the graph node repository cannot be initialized
     */
    private GraphManagerFactory createGraphManagerFactory(
            ProjectRepository repository,
            StorageManagerInterface storageManager,
            PipelineManagerFactoryInterface pipelineManagerFactory
    ) throws OperationIncompleteException {
        GraphManagerNodeRepositoryInterface nodeRepository =
                new GraphManagerNodeRepository(repository);
        return new GraphManagerFactory(storageManager, nodeRepository, pipelineManagerFactory);
    }

    /**
     * Creates the factory responsible for project service instances.
     *
     * @param repository project repository
     * @param serviceLoader project service loader
     * @param storageManager project storage manager
     * @return service manager factory for the revision
     */
    private ServicesManagerFactory createServicesManagerFactory(
            ProjectRepository repository,
            ServiceLoaderInterface serviceLoader,
            StorageManagerInterface storageManager
    ) {
        return new ServicesManagerFactory(repository, serviceLoader, storageManager);
    }

    /**
     * Creates the orchestration service connecting project services and graph execution.
     *
     * @param servicesManagerFactory project service manager factory
     * @param graphManagerFactory project graph manager factory
     * @return orchestration service for the revision
     */
    private OrchestrationService createOrchestrationService(
            ServicesManagerFactory servicesManagerFactory,
            GraphManagerFactory graphManagerFactory
    ) {
        return new OrchestrationService(servicesManagerFactory, graphManagerFactory);
    }

    /**
     * Creates the lifecycle owner for all resources belonging to one project revision.
     *
     * @param revisionId immutable revision identifier
     * @param orchestrationService project orchestration service
     * @param storageManager project storage manager
     * @param moduleLoader project module loader
     * @param serviceLoader project service loader
     * @return isolated project runtime
     */
    private ProjectRuntime createProjectRuntime(
            String revisionId,
            OrchestrationService orchestrationService,
            StorageManagerInterface storageManager,
            ModuleLoaderInterface moduleLoader,
            ServiceLoaderInterface serviceLoader
    ) {
        return new DefaultProjectRuntime(
                revisionId,
                orchestrationService,
                storageManager,
                moduleLoader,
                serviceLoader
        );
    }

}
