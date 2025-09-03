package io.github.byzatic.tessera.engine.infrastructure.config;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationService;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.domain.business.sheduller.Scheduler;
import io.github.byzatic.tessera.engine.domain.business.sheduller.SchedulerInterface;
import io.github.byzatic.tessera.engine.domain.repository.*;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_global_dao.NodeGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao.PipelineDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.path_manager.StructureManager;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.path_manager.StructureManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.ProjectDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_global.ProjectGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository.JpaLikeNodeGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository.NodeGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository.JpaLikeNodeRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository.ProjectDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.JpaLikePipelineRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository.PipelineDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository.JpaLikeProjectGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository.ProjectGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.shared_resources_manager.SharedResourcesRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.storage_manager.StorageManager;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.GraphManager;
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
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.ServicesManager;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.ServicesManagerFactory;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoader;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoaderInterface;

public class ApplicationContext {
    private static OrchestrationServiceInterface orchestrationServiceInterface = null;
    private static SchedulerInterface domainLogicScheduler = null;
    private static ServicesManagerInterface serviceManager = null;
    private static JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository = null;
    private static ServiceLoaderInterface serviceLoader = null;
    private static StorageManagerInterface storageManager = null;
    private static io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller.SchedulerInterface serviceManagerScheduler = null;
    private static JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository = null;
    private static JpaLikeNodeRepositoryInterface nodeRepository = null;
    private static GraphManagerInterface graphManager = null;
    private static GraphManagerNodeRepositoryInterface graphManagerNodeRepository = null;
    private static io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.SchedulerInterface graphManagerScheduler = null;
    private static PipelineManagerFactoryInterface pipelineManagerFactory = null;
    private static JpaLikePipelineRepositoryInterface pipelineRepository = null;
    private static ModuleLoaderInterface moduleLoader = null;
    private static PathManagerInterface pathManager = null;
    private static SharedResourcesRepositoryInterface sharedResourcesManager = null;
    private static ExecutionContextFactory executionContextFactory = null;
    private static GraphPathManagerInterface graphPathManager = null;
    private static ProjectDaoInterface projectDao = null;
    private static NodeGlobalDaoInterface nodeGlobalDaoInterface = null;
    private static PipelineDaoInterface pipelineDao = null;
    private static ProjectGlobalDaoInterface projectGlobalDao= null;
    private static StructureManagerInterface structureManager= null;
    private static ServicesManagerFactoryInterface servicesManagerFactory;
    private static GraphManagerFactoryInterface graphManagerFactory;

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

    public static SchedulerInterface getDomainLogicScheduler() {
        if (domainLogicScheduler == null) {
            domainLogicScheduler = new Scheduler();
        }
        return domainLogicScheduler;
    }

    public static ServicesManagerInterface getServiceManager() {
        if (serviceManager == null) {
            serviceManager = new ServicesManager(
                    getProjectGlobalRepository(),
                    getServiceLoader(),
                    getStorageManager(),
                    getNodeRepository()
            );
        }
        return serviceManager;
    }

    public static ServicesManagerFactoryInterface getServicesManagerFactory() {
        if (servicesManagerFactory == null) {
            servicesManagerFactory = new ServicesManagerFactory(
                    getProjectGlobalRepository(),
                    getServiceLoader(),
                    getStorageManager(),
                    getNodeRepository()
            );
        }
        return servicesManagerFactory;
    }

    public static io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller.SchedulerInterface getServiceManagerScheduler() {
        if (serviceManagerScheduler == null) {
            serviceManagerScheduler = new io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller.Scheduler();
        }
        return serviceManagerScheduler;
    }

    public static JpaLikeProjectGlobalRepositoryInterface getProjectGlobalRepository() {
        try {
            if (projectGlobalRepository == null) {
                projectGlobalRepository = new JpaLikeProjectGlobalRepository(Configuration.PROJECT_NAME, getProjectGlobalDao());
            }
            return projectGlobalRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ProjectGlobalDaoInterface getProjectGlobalDao() {
        try {
            if (projectGlobalDao == null) {
                projectGlobalDao = new ProjectGlobalDao();
            }
            return projectGlobalDao;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ServiceLoaderInterface getServiceLoader() {
        try {
            if (serviceLoader == null) {
                serviceLoader = new ServiceLoader(
                        Configuration.PROJECT_SERVICES_PATH,
                        getSharedResourcesManager()
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
                        getNodeGlobalRepository(),
                        getProjectGlobalRepository(),
                        getNodeRepository()
                );
            }
            return storageManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JpaLikeNodeGlobalRepositoryInterface getNodeGlobalRepository() {
        try {
            if (nodeGlobalRepository == null) {
                nodeGlobalRepository = new JpaLikeNodeGlobalRepository(
                        Configuration.PROJECT_NAME,
                        getNodeGlobalDao()
                );
            }
            return nodeGlobalRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JpaLikeNodeRepositoryInterface getNodeRepository() {
        try {
            if (nodeRepository == null) {
                nodeRepository = new JpaLikeNodeRepository(Configuration.PROJECT_NAME, getProjectDao());
            }
            return nodeRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ProjectDaoInterface getProjectDao() {
        if (projectDao == null) {
            projectDao = new ProjectDao();
        }
        return projectDao;
    }

    public static NodeGlobalDaoInterface getNodeGlobalDao() {
        if (nodeGlobalDaoInterface == null) {
            nodeGlobalDaoInterface = new NodeGlobalDao(getNodeRepository(), getStructureManager());
        }
        return nodeGlobalDaoInterface;
    }


    public static GraphManagerInterface getGraphManager() {
        if (graphManager == null) {
            graphManager = new GraphManager(
                    getGraphManagerNodeRepository(),
                    getPipelineManagerFactory()
            );
        }
        return graphManager;
    }

    public static GraphManagerFactoryInterface getGraphManagerFactory() {
        if (graphManagerFactory == null) {
            graphManagerFactory = new GraphManagerFactory(
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
                        getNodeRepository()
                );
            }
            return graphManagerNodeRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.SchedulerInterface getGraphManagerScheduler() {
        if (graphManagerScheduler == null) {
            graphManagerScheduler = new io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.Scheduler();
        }
        return graphManagerScheduler;
    }

    public static PipelineManagerFactoryInterface getPipelineManagerFactory() {
        if (pipelineManagerFactory == null) {
            pipelineManagerFactory = new PipelineManagerFactory(
                    getPipelineRepository(),
                    getNodeRepository(),
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
                    getNodeRepository(),
                    getProjectGlobalRepository(),
                    getNodeGlobalRepository(),
                    getGraphPathManager()
            );
        }
        return executionContextFactory;
    }

    public static GraphPathManagerInterface getGraphPathManager() {
        if (graphPathManager == null) {
            graphPathManager = new GraphPathManager(
                    getNodeRepository()
            );
        }
        return graphPathManager;
    }

    public static JpaLikePipelineRepositoryInterface getPipelineRepository() {
        try {
            if (pipelineRepository == null) {
                pipelineRepository = new JpaLikePipelineRepository(
                        Configuration.PROJECT_NAME,
                        getPipelineDao()
                );
            }
            return pipelineRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static PipelineDaoInterface getPipelineDao() {
        try {
            if (pipelineDao == null) {
                pipelineDao = new PipelineDao(
                        getNodeRepository(),
                        getStructureManager()
                );
            }
            return pipelineDao;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StructureManagerInterface getStructureManager() {
        try {
            if (structureManager == null) {
                structureManager = new StructureManager(
                        getNodeRepository(),
                        Configuration.PROJECT_NAME
                );
            }
            return structureManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ModuleLoaderInterface getModuleLoader() {
        try {
            if (moduleLoader == null) {
                moduleLoader = new ModuleLoader(
                        Configuration.PROJECT_WORKFLOW_ROUTINES_PATH,
                        getSharedResourcesManager()
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
                        getNodeRepository(),
                        getStructureManager()
                );
            }
            return pathManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SharedResourcesRepositoryInterface getSharedResourcesManager() {
        try {
            if (sharedResourcesManager == null) {
                sharedResourcesManager = new SharedResourcesRepository(Configuration.PROJECT_NAME);
            }
            return sharedResourcesManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
