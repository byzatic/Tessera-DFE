package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.application.revision.ProjectRevision;
import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.lib.configio.domain.exception.PluginLoadingException;
import io.github.byzatic.lib.configio.infrastructure.factory.ModuleLoaderFactory;
import io.github.byzatic.lib.configio.infrastructure.factory.ServiceLoaderFactory;
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
    public ProjectRuntime create(ProjectRevision revision)
            throws OperationIncompleteException {
        Objects.requireNonNull(revision, "revision");

        ModuleLoaderInterface moduleLoader = null;
        ServiceLoaderInterface serviceLoader = null;
        try {
            Path projectDirectory = revision.getProjectDirectory();
            ProjectRepository repository = createProjectRepository(revision);
            ClassLoader sharedResourcesClassLoader = repository.getSharedResourcesClassLoader();

            serviceLoader = createServiceLoader(projectDirectory, sharedResourcesClassLoader);
            moduleLoader = createModuleLoader(projectDirectory, sharedResourcesClassLoader);

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
            closeAfterFailure(moduleLoader, serviceLoader, exception);
            throw new OperationIncompleteException(
                    "Cannot create runtime for revision " + revision.getRevisionId(),
                    exception
            );
        }
    }

    /**
     * Creates the repository backed by the project data already loaded for the revision.
     *
     * @param revision prepared project revision
     * @return repository for the revision
     * @throws OperationIncompleteException when the loaded project cannot be mapped
     */
    private ProjectRepository createProjectRepository(ProjectRevision revision)
            throws OperationIncompleteException {
        return new ProjectRepositoryImpl(revision.getProject());
    }

    /**
     * Creates the service loader for the staged project directory.
     *
     * @param projectDirectory project root directory
     * @param sharedResourcesClassLoader class loader containing project shared resources
     * @return service loader owned by the project runtime
     * @throws PluginLoadingException when service discovery cannot be initialized
     */
    private ServiceLoaderInterface createServiceLoader(
            Path projectDirectory,
            ClassLoader sharedResourcesClassLoader
    ) throws PluginLoadingException {
        Path servicesDirectory = projectDirectory.resolve("modules").resolve("services");
        return ServiceLoaderFactory.create(servicesDirectory, sharedResourcesClassLoader);
    }

    /**
     * Creates the workflow routine loader for the staged project directory.
     *
     * @param projectDirectory project root directory
     * @param sharedResourcesClassLoader class loader containing project shared resources
     * @return module loader owned by the project runtime
     * @throws PluginLoadingException when module discovery cannot be initialized
     */
    private ModuleLoaderInterface createModuleLoader(
            Path projectDirectory,
            ClassLoader sharedResourcesClassLoader
    ) throws PluginLoadingException {
        Path modulesDirectory = projectDirectory
                .resolve("modules")
                .resolve("workflow_routines");
        return ModuleLoaderFactory.create(modulesDirectory, sharedResourcesClassLoader);
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

    /**
     * Closes loaders created before a runtime construction failure.
     *
     * @param moduleLoader module loader, or {@code null} when it was not created
     * @param serviceLoader service loader, or {@code null} when it was not created
     * @param failure construction failure receiving suppressed cleanup errors
     */
    private void closeAfterFailure(
            ModuleLoaderInterface moduleLoader,
            ServiceLoaderInterface serviceLoader,
            Throwable failure
    ) {
        if (moduleLoader != null) {
            try {
                moduleLoader.close();
            } catch (Exception exception) {
                failure.addSuppressed(exception);
            }
        }
        if (serviceLoader != null) {
            try {
                serviceLoader.close();
            } catch (Exception exception) {
                failure.addSuppressed(exception);
            }
        }
    }
}
