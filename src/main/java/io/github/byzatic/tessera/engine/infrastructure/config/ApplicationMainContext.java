package io.github.byzatic.tessera.engine.infrastructure.config;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationService;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.*;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.ResourceFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.ResourceManager;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.common_v1_hierarchical.ResourceFactoryV1Hierarchical;
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
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader.ModuleLoader;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader.ModuleLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.ServicesManagerFactory;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoader;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoaderInterface;

public class ApplicationMainContext {
    private static OrchestrationServiceInterface orchestrationServiceInterface = null;
    private static ServiceLoaderInterface serviceLoader = null;
    private static StorageManagerInterface storageManager = null;
    private static GraphManagerNodeRepositoryInterface graphManagerNodeRepository = null;
    private static PipelineManagerFactoryInterface pipelineManagerFactory = null;
    private static ModuleLoaderInterface moduleLoader = null;
    private static PathManagerInterface pathManager = null;
    private static ExecutionContextFactory executionContextFactory = null;
    private static GraphPathManagerInterface graphPathManager = null;
    private static ServicesManagerFactoryInterface servicesManagerFactory = null;
    private static GraphManagerFactoryInterface graphManagerFactory = null;
    private static ResourceFactoryInterface resourceFactory = null;
    private static ResourceManagerInterface resourceManager = null;

    static {
        Configuration.MDC_ENGINE_CONTEXT.apply();
    }

    public static OrchestrationServiceInterface getDomainLogic() {
        if (orchestrationServiceInterface == null) {
            orchestrationServiceInterface = new OrchestrationService(
                    getServicesManagerFactory(),
                    getGraphManagerFactory()
            );
        }
        return orchestrationServiceInterface;
    }


    public static ServicesManagerFactoryInterface getServicesManagerFactory() {
        if (servicesManagerFactory == null) {
            servicesManagerFactory = new ServicesManagerFactory(
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeProjectGlobalRepositoryInterface.class),
                    getServiceLoader(),
                    getStorageManager(),
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class)
            );
        }
        return servicesManagerFactory;
    }

    public static ServiceLoaderInterface getServiceLoader() {
        try {
            if (serviceLoader == null) {
                serviceLoader = new ServiceLoader(
                        Configuration.PROJECT_SERVICES_PATH,
                        getResourceManager().getResource(Configuration.PROJECT_NAME, SharedResourcesRepositoryInterface.class)
                );
            }
            return serviceLoader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StorageManagerInterface getStorageManager() {
        try {
            if (storageManager == null) {
                storageManager = new StorageManager(
                        getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeGlobalRepositoryInterface.class),
                        getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeProjectGlobalRepositoryInterface.class),
                        getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class)
                );
            }
            return storageManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static GraphManagerFactoryInterface getGraphManagerFactory() {
        if (graphManagerFactory == null) {
            graphManagerFactory = new GraphManagerFactory(
                    getStorageManager(),
                    getGraphManagerNodeRepository(),
                    getPipelineManagerFactory()
            );
        }
        return graphManagerFactory;
    }

    public static GraphManagerNodeRepositoryInterface getGraphManagerNodeRepository() {
        try {
            if (graphManagerNodeRepository == null) {
                graphManagerNodeRepository = new GraphManagerNodeRepository(
                        getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class)
                );
            }
            return graphManagerNodeRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PipelineManagerFactoryInterface getPipelineManagerFactory() {
        if (pipelineManagerFactory == null) {
            pipelineManagerFactory = new PipelineManagerFactory(
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikePipelineRepositoryInterface.class),
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class),
                    getModuleLoader(),
                    getStorageManager(),
                    getPathManager(),
                    getExecutionContextFactory()
            );
        }
        return pipelineManagerFactory;
    }

    public static ExecutionContextFactoryInterface getExecutionContextFactory() {
        if (executionContextFactory == null) {
            executionContextFactory = new ExecutionContextFactory(
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class),
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeProjectGlobalRepositoryInterface.class),
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeGlobalRepositoryInterface.class),
                    getGraphPathManager()
            );
        }
        return executionContextFactory;
    }

    public static GraphPathManagerInterface getGraphPathManager() {
        if (graphPathManager == null) {
            graphPathManager = new GraphPathManager(
                    getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class)
            );
        }
        return graphPathManager;
    }

    public static ModuleLoaderInterface getModuleLoader() {
        try {
            if (moduleLoader == null) {
                moduleLoader = new ModuleLoader(
                        Configuration.PROJECT_WORKFLOW_ROUTINES_PATH,
                        getResourceManager().getResource(Configuration.PROJECT_NAME, SharedResourcesRepositoryInterface.class)
                );
            }
            return moduleLoader;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PathManagerInterface getPathManager() {
        try {
            if (pathManager == null) {
                pathManager = new PathManager(
                        getResourceManager().getResource(Configuration.PROJECT_NAME, JpaLikeNodeRepositoryInterface.class),
                        getResourceManager().getResource(Configuration.PROJECT_NAME, StructureControllerInterface.class)
                );
            }
            return pathManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ResourceManagerInterface getResourceManager() {
        try {
            if (resourceManager == null) {
                resourceManager = new ResourceManager(
                        getResourceFactory()
                );
                resourceManager.loadAll(Configuration.PROJECT_NAME);
            }
            return resourceManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ResourceFactoryInterface getResourceFactory() {
        try {
            if (resourceFactory == null) {
                resourceFactory = new ResourceFactoryV1Hierarchical();
            }
            return resourceFactory;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
