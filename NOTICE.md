
tests issue

```shell

SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.

Process finished with exit code 0

```

To move from MCg3 libs to Tessera libs
```shell
find . -type f \( -name '*.java' -o -name '*.xml' -o -name '*.properties' \) \
  -exec sed -i '' 's/ru\.byzatic\.metrics_core\.mcg3_enginecommon_lib/io.github.byzatic.tessera.enginecommon/g' {} +

find . -type f \( -name '*.java' -o -name '*.xml' -o -name '*.properties' \) \
  -exec sed -i '' 's/ru\.byzatic\.metrics_core\.mcg3_storageapi_lib/io.github.byzatic.tessera.storageapi/g' {} +

find . -type f \( -name '*.java' -o -name '*.xml' -o -name '*.properties' \) \
  -exec sed -i '' 's/ru\.byzatic\.metrics_core\.service_lib/io.github.byzatic.tessera.service/g' {} +

find . -type f \( -name '*.java' -o -name '*.xml' -o -name '*.properties' \) \
  -exec sed -i '' 's/ru\.byzatic\.metrics_core\.workflowroutines_lib/io.github.byzatic.tessera.workflowroutine/g' {} +
```

```shell

        изменено:      src/main/java/io/github/byzatic/tessera/engine/Configuration.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/application/commons/logging/MdcEngineContext.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/application/commons/logging/MdcServiceContext.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/application/commons/logging/MdcWorkflowRoutineContext.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/domain/repository/storage/GlobalStorageManagerInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/domain/repository/storage/NodeStorageManagerInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/shared_resources_manager/SharedResourcesRepository.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/storage/Storage.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/PipelineManager.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/MCg3WorkflowRoutineApi.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/StorageApi.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/ExecutionContext.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/ExecutionContextFactory.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/ExecutionContextFactoryInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/GraphPath.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/NodeDescription.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/PipelineDescription.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/PipelineExecutionInfo.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/StorageDescription.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/StorageOption.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/module_loader/ModuleLoader.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/module_loader/ModuleLoaderInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/sheduller/JobDetail.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/sheduller/Scheduler.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/ServicesManager.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/service_api_interface/ExecutionContext.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/service_api_interface/MCg3ServiceApi.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/service_api_interface/StorageApi.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/service_loader/ServiceLoader.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/service_loader/ServiceLoaderInterface.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/sheduller/JobDetail.java
        изменено:      src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/sheduller/Scheduler.java


```