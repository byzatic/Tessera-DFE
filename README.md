# Tessera Data Flow Engine
Tessera Data Flow Engine is a modular execution system based on directed acyclic graphs (DAGs), designed to build flexible and extensible data processing pipelines. Each module (or “tessera” tile) represents an independent node that processes data and passes the result along the execution graph.

**FULL README coming soon**

```
./src
├── main
│   ├── java
│   │   └── io.github.byzatic.tessera
│   │       └── engine
│   │           ├── App.java
│   │           ├── application
│   │           │   ├── commons
│   │           │   │   ├── exceptions
│   │           │   │   │   ├── BusinessLogicException.java
│   │           │   │   │   ├── doc.txt
│   │           │   │   │   ├── ExternalProcessException.java
│   │           │   │   │   ├── NotFoundException.java
│   │           │   │   │   ├── OperationIncompleteException.java
│   │           │   │   │   ├── OperationTimedOutException.java
│   │           │   │   │   └── ValidationException.java
│   │           │   │   └── logging
│   │           │   │       ├── MdcEngineContext.java
│   │           │   │       ├── MdcServiceContext.java
│   │           │   │       └── MdcWorkflowRoutineContext.java
│   │           │   └── usecase
│   │           │       └── ProjectOrchestrator.java
│   │           ├── Configuration.java
│   │           ├── domain
│   │           │   ├── business
│   │           │   │   ├── GraphManagerHandler.java
│   │           │   │   ├── OrchestrationService.java
│   │           │   │   ├── OrchestrationServiceInterface.java
│   │           │   │   └── sheduller
│   │           │   │       ├── CronDateCalculator.java
│   │           │   │       ├── JobDetail.java
│   │           │   │       ├── JobWrapper.java
│   │           │   │       ├── Scheduler.java
│   │           │   │       ├── SchedulerInterface.java
│   │           │   │       ├── StatusProxy.java
│   │           │   │       └── StatusResult.java
│   │           │   ├── model
│   │           │   │   ├── DataLookupIdentifierImpl.java
│   │           │   │   ├── GraphNodeRef.java
│   │           │   │   ├── node
│   │           │   │   │   ├── NodeItem.java
│   │           │   │   │   └── Project.java
│   │           │   │   ├── node_global
│   │           │   │   │   ├── NodeGlobal.java
│   │           │   │   │   ├── OptionsItem.java
│   │           │   │   │   └── StoragesItem.java
│   │           │   │   ├── node_pipeline
│   │           │   │   │   ├── ConfigurationFilesItem.java
│   │           │   │   │   ├── NodePipeline.java
│   │           │   │   │   ├── StagesConsistencyItem.java
│   │           │   │   │   ├── StagesDescriptionItem.java
│   │           │   │   │   └── WorkersDescriptionItem.java
│   │           │   │   └── project
│   │           │   │       ├── ProjectGlobal.java
│   │           │   │       ├── ServiceItem.java
│   │           │   │       ├── ServicesOptionsItem.java
│   │           │   │       ├── StoragesItem.java
│   │           │   │       └── StoragesOptionsItem.java
│   │           │   ├── repository
│   │           │   │   ├── JpaLikeNodeGlobalRepositoryInterface.java
│   │           │   │   ├── JpaLikeNodeRepositoryInterface.java
│   │           │   │   ├── JpaLikePipelineRepositoryInterface.java
│   │           │   │   ├── JpaLikeProjectGlobalRepositoryInterface.java
│   │           │   │   ├── SharedResourcesRepositoryInterface.java
│   │           │   │   └── storage
│   │           │   │       ├── GlobalStorageManagerInterface.java
│   │           │   │       ├── NodeStorageManagerInterface.java
│   │           │   │       └── StorageManagerInterface.java
│   │           │   └── service
│   │           │       ├── GraphManagerInterface.java
│   │           │       └── ServicesManagerInterface.java
│   │           ├── infrastructure
│   │           │   ├── config
│   │           │   │   └── ApplicationContext.java
│   │           │   ├── persistence
│   │           │   │   ├── configuration_dao
│   │           │   │   │   ├── hierarchical_directed_acyclic_graph
│   │           │   │   │   │   └── HierarchicalDirectedAcyclicGraph.java
│   │           │   │   │   └── single_root_strict_nested_node_tree
│   │           │   │   │       ├── node_global_dao
│   │           │   │   │       │   ├── NodeGlobalDao.java
│   │           │   │   │       │   └── SupportNodeGlobalLoader.java
│   │           │   │   │       ├── node_pipeline_dao
│   │           │   │   │       │   ├── PipelineDao.java
│   │           │   │   │       │   └── SupportNodePipelineLoader.java
│   │           │   │   │       ├── path_manager
│   │           │   │   │       │   ├── NodeStructure.java
│   │           │   │   │       │   ├── ProjectStructure.java
│   │           │   │   │       │   ├── StructureManager.java
│   │           │   │   │       │   └── StructureManagerInterface.java
│   │           │   │   │       ├── project
│   │           │   │   │       │   ├── dto
│   │           │   │   │       │   │   ├── ConfigNodeItem.java
│   │           │   │   │       │   │   └── ConfigProject.java
│   │           │   │   │       │   ├── ProjectDao.java
│   │           │   │   │       │   └── SupportNodesStructureCompressor.java
│   │           │   │   │       ├── project_global
│   │           │   │   │       │   ├── ProjectGlobalDao.java
│   │           │   │   │       │   └── SupportProjectGlobalLoader.java
│   │           │   │   │       └── SingleRootStrictNestedNodeTree.java
│   │           │   │   ├── jpa_like_node_global_repository
│   │           │   │   │   ├── JpaLikeNodeGlobalRepository.java
│   │           │   │   │   └── NodeGlobalDaoInterface.java
│   │           │   │   ├── jpa_like_node_repository
│   │           │   │   │   ├── JpaLikeNodeRepository.java
│   │           │   │   │   ├── ProjectDaoInterface.java
│   │           │   │   │   └── SupportProjectValidation.java
│   │           │   │   ├── jpa_like_pipeline_repository
│   │           │   │   │   ├── JpaLikePipelineRepository.java
│   │           │   │   │   └── PipelineDaoInterface.java
│   │           │   │   ├── jpa_like_project_global_repository
│   │           │   │   │   ├── JpaLikeProjectGlobalRepository.java
│   │           │   │   │   └── ProjectGlobalDaoInterface.java
│   │           │   │   ├── shared_resources_manager
│   │           │   │   │   ├── CompositeClassLoader.java
│   │           │   │   │   └── SharedResourcesRepository.java
│   │           │   │   └── storage_manager
│   │           │   │       ├── storage
│   │           │   │       │   └── Storage.java
│   │           │   │       ├── StorageInterface.java
│   │           │   │       └── StorageManager.java
│   │           │   └── service
│   │           │       ├── graph_reactor
│   │           │       │   ├── dto
│   │           │       │   │   └── Node.java
│   │           │       │   └── graph_manager
│   │           │       │       ├── graph_management
│   │           │       │       │   ├── GraphPathFinderIterative.java
│   │           │       │       │   ├── GraphPathManager.java
│   │           │       │       │   └── GraphPathManagerInterface.java
│   │           │       │       ├── graph_path_manager
│   │           │       │       │   ├── PathManager.java
│   │           │       │       │   └── PathManagerInterface.java
│   │           │       │       ├── graph_traversal
│   │           │       │       │   ├── GraphTraversal.java
│   │           │       │       │   ├── GraphTraversalInterface.java
│   │           │       │       │   ├── node_repository
│   │           │       │       │   │   ├── GraphManagerNodeRepository.java
│   │           │       │       │   │   └── GraphManagerNodeRepositoryInterface.java
│   │           │       │       │   ├── NodeLifecycleState.java
│   │           │       │       │   ├── NodePathState.java
│   │           │       │       │   └── sheduller
│   │           │       │       │       ├── health
│   │           │       │       │       │   ├── HealthFlag.java
│   │           │       │       │       │   └── HealthStateProxy.java
│   │           │       │       │       ├── job
│   │           │       │       │       │   ├── Job.java
│   │           │       │       │       │   └── JobInterface.java
│   │           │       │       │       ├── JobDetail.java
│   │           │       │       │       ├── Scheduler.java
│   │           │       │       │       └── SchedulerInterface.java
│   │           │       │       ├── GraphManager.java
│   │           │       │       └── pipeline_manager
│   │           │       │           ├── api_interface
│   │           │       │           │   ├── execution_context
│   │           │       │           │   │   ├── ExecutionContext.java
│   │           │       │           │   │   ├── ExecutionContextFactory.java
│   │           │       │           │   │   ├── ExecutionContextFactoryInterface.java
│   │           │       │           │   │   ├── GraphPath.java
│   │           │       │           │   │   ├── NodeDescription.java
│   │           │       │           │   │   ├── PipelineDescription.java
│   │           │       │           │   │   ├── PipelineExecutionInfo.java
│   │           │       │           │   │   ├── StorageDescription.java
│   │           │       │           │   │   └── StorageOption.java
│   │           │       │           │   ├── MCg3WorkflowRoutineApi.java
│   │           │       │           │   └── StorageApi.java
│   │           │       │           ├── module_loader
│   │           │       │           │   ├── ModuleLoader.java
│   │           │       │           │   └── ModuleLoaderInterface.java
│   │           │       │           ├── PipelineManager.java
│   │           │       │           ├── PipelineManagerFactory.java
│   │           │       │           ├── PipelineManagerFactoryInterface.java
│   │           │       │           ├── PipelineManagerInterface.java
│   │           │       │           ├── sheduller
│   │           │       │           │   ├── JobDetail.java
│   │           │       │           │   ├── Scheduler.java
│   │           │       │           │   └── SchedulerInterface.java
│   │           │       │           └── SupportPathResolver.java
│   │           │       └── service_manager
│   │           │           ├── dto
│   │           │           │   ├── ServiceDescriptor.java
│   │           │           │   └── ServiceParameter.java
│   │           │           ├── service_api_interface
│   │           │           │   ├── ExecutionContext.java
│   │           │           │   ├── MCg3ServiceApi.java
│   │           │           │   └── StorageApi.java
│   │           │           ├── service_loader
│   │           │           │   ├── ServiceLoader.java
│   │           │           │   └── ServiceLoaderInterface.java
│   │           │           ├── ServicesManager.java
│   │           │           └── sheduller
│   │           │               ├── JobDetail.java
│   │           │               ├── Scheduler.java
│   │           │               └── SchedulerInterface.java
│   │           └── modules
│   └── resources
│       └── logback.xml
└── test
    ├── java
    │   └── io.github.byzatic.tessera
    │       └── engine
    │           └── graph_reactor
    │               └── dto
    │                   └── graph_manager
    │                       ├── GraphTraversalTest.java
    │                       └── node_repository
    │                           ├── GraphManagerNodeRepositoryTest.java
    │                           └── node_repository
    │                               └── GraphManagerNodeRepositoryTest.java
    └── resources
        └── logback-test.xml

```

```
src/
└── main/
    └── java/
        └── com/
            └── example/
                └── graphproject/
                    ├── domain/
                    │   ├── model/
                    │   │   ├── Project.java
                    │   │   ├── Graph.java
                    │   │   └── Node.java
                    │   │
                    │   ├── repository/
                    │   │   ├── ProjectRepositoryInterface.java
                    │   │   ├── GraphRepositoryInterface.java
                    │   │   └── NodeRepositoryInterface.java
                    │   │
                    │   ├── service/
                    │   │   ├── GraphManagerInterface.java
                    │   │   └── ServiceManagerInterface.java
                    │   │
                    │   └── business/
                    │       └── OrchestrationService.java
                    │
                    ├── application/
                    │   └── usecase/
                    │       └── ProjectOrchestrator.java
                    │
                    ├── infrastructure/
                    │   ├── persistence/
                    │   │   ├── JpaProjectRepository.java
                    │   │   ├── JpaGraphRepository.java
                    │   │   └── JpaNodeRepository.java
                    │   │
                    │   ├── service/
                    │   │   ├── DefaultGraphManager.java
                    │   │   └── DefaultServiceManager.java
                    │   │
                    │   └── config/
                    │       └── BeanConfiguration.java
                    │
                    └── adapter/
                        └── web/
                            └── ProjectController.java
```
| Путь                       | Описание                                                     |
|---------------------------|--------------------------------------------------------------|
| `domain/model`            | Сущности предметной области                                  |
| `domain/repository`       | Контракты хранилищ (интерфейсы)                              |
| `domain/service`          | Контракты менеджеров (интерфейсы)                            |
| `domain/business`         | Централизованная бизнес-логика, orchestration                |
| `application/usecase`     | Сценарии использования — связывают бизнес-логику и интерфейсы |
| `infrastructure/persistence` | JPA-реализации репозиториев                             |
| `infrastructure/service`  | Реализации интерфейсов менеджеров                            |
| `infrastructure/config`   | Конфигурация биндинга интерфейсов на реализации              |
| `adapter/web`             | REST API или другие входные точки                            |