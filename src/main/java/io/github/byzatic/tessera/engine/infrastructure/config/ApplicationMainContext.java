package io.github.byzatic.tessera.engine.infrastructure.config;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationService;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.domain.repository.ProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_global_dao.NodeGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao.PipelineDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.ProjectDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_global.ProjectGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureController;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.shared_resources_dao.SharedResourcesDAO;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.*;
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
    private static ProjectRepository fullProjectRepository;
    private static StructureControllerInterface structureController;
    private static ProjectDaoInterface projectDao;
    private static NodeGlobalDaoInterface nodeGlobalDao;
    private static PipelineDaoInterface pipelineDao;
    private static ProjectGlobalDaoInterface projectGlobalDao;
    private static SharedResourcesDAOInterface sharedResourcesDAO;

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
                    getProjectRepository(),
                    getServiceLoader(),
                    getStorageManager()
            );
        }
        return servicesManagerFactory;
    }

    public static ServiceLoaderInterface getServiceLoader() {
        try {
            if (serviceLoader == null) {
                serviceLoader = new ServiceLoader(
                        Configuration.PROJECT_SERVICES_PATH,
                        getProjectRepository()
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
                        getProjectRepository()
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
                        getProjectRepository()
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
                    getProjectRepository(),
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
                    getProjectRepository(),
                    getGraphPathManager()
            );
        }
        return executionContextFactory;
    }

    public static GraphPathManagerInterface getGraphPathManager() {
        if (graphPathManager == null) {
            graphPathManager = new GraphPathManager(
                    getProjectRepository()
            );
        }
        return graphPathManager;
    }

    public static ModuleLoaderInterface getModuleLoader() {
        try {
            if (moduleLoader == null) {
                moduleLoader = new ModuleLoader(
                        Configuration.PROJECT_WORKFLOW_ROUTINES_PATH,
                        getProjectRepository()
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
                        getProjectRepository(),
                        getStructureController()
                );
            }
            return pathManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ProjectRepository getProjectRepository() {
        try {
            if (fullProjectRepository == null) {
                fullProjectRepository = new ProjectRepositoryImpl(
                        Configuration.PROJECT_NAME
                );
                fullProjectRepository.addProjectLoader(ProjectRepository.ProjectLoaderTypes.PLV1, new ProjectV1Loader(
                                getNodeGlobalDao(),
                                getPipelineDao(),
                                getProjectDao(),
                                getProjectGlobalDao(),
                                getSharedResourcesDAO()
                        )
                );
                fullProjectRepository.load();
            }
            return fullProjectRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ProjectDaoInterface getProjectDao() {
        if (projectDao == null) {
            projectDao = new ProjectDao(getStructureController());
        }
        return projectDao;
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

    public static NodeGlobalDaoInterface getNodeGlobalDao() {
        if (nodeGlobalDao == null) {
            nodeGlobalDao = new NodeGlobalDao(getStructureController());
        }
        return nodeGlobalDao;
    }

    public static PipelineDaoInterface getPipelineDao() {
        try {
            if (pipelineDao == null) {
                pipelineDao = new PipelineDao(getStructureController());
            }
            return pipelineDao;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SharedResourcesDAOInterface getSharedResourcesDAO() {
        try {
            if (sharedResourcesDAO == null) {
                sharedResourcesDAO = new SharedResourcesDAO();
            }
            return sharedResourcesDAO;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static StructureControllerInterface getStructureController() {
        try {
            if (structureController == null) {
                structureController = new StructureController(Configuration.PROJECT_NAME);
            }
            return structureController;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
