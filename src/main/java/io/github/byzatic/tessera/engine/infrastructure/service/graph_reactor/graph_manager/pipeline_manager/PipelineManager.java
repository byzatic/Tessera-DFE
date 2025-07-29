package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.*;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.MCg3WorkflowRoutineApi;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.StorageApi;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContextFactoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader.ModuleLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.sheduller.JobDetail;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.sheduller.Scheduler;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.sheduller.SchedulerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_path_manager.PathManagerInterface;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikePipelineRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.workflowroutine.configuration.ConfigurationParameter;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagState;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PipelineManager implements PipelineManagerInterface {
    private final static Logger logger= LoggerFactory.getLogger(PipelineManager.class);
    private final SchedulerInterface scheduler;
    private GraphNodeRef graphNodeRef = null;
    private List<GraphNodeRef> pathToCurrentExecutionNodeRef = null;
    private StorageManagerInterface storageManager = null;
    private ModuleLoaderInterface moduleLoader = null;
    private JpaLikePipelineRepositoryInterface pipelineRepository = null;
    private JpaLikeNodeRepositoryInterface nodeRepository = null;
    private SupportPathResolver pathResolver = null;
    private ExecutionContextFactoryInterface executionContextFactory = null;

    public PipelineManager(GraphNodeRef graphNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, JpaLikePipelineRepositoryInterface pipelineRepository, JpaLikeNodeRepositoryInterface nodeRepository, ModuleLoaderInterface moduleLoader, StorageManagerInterface storageManager, PathManagerInterface pathManagerInterface, ExecutionContextFactoryInterface executionContextFactory) throws OperationIncompleteException {
        this.graphNodeRef = graphNodeRef;
        this.pathToCurrentExecutionNodeRef = pathToCurrentExecutionNodeRef;
        this.pipelineRepository = pipelineRepository;
        this.nodeRepository = nodeRepository;
        this.moduleLoader = moduleLoader;
        this.storageManager = storageManager;
        this.scheduler = new Scheduler();
        this.executionContextFactory = executionContextFactory;
        try {
            this.pathResolver = new SupportPathResolver(pathManagerInterface.getStoragePathByGraphNodeRef(graphNodeRef), pathManagerInterface.getProjectGlobalStorage());
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public void runPipeline() throws OperationIncompleteException {
        logger.debug("Run Pipeline for jpa_like_node_repository {}", nodeRepository.getNode(graphNodeRef).getName());

        // Sort stagesConsistencyItemList by Position
        NodePipeline pipeline = pipelineRepository.getPipeline(graphNodeRef);
        List<StagesConsistencyItem> stagesConsistencyItemList = new ArrayList<>(pipeline.getStagesConsistency());
        stagesConsistencyItemList.sort(Comparator.comparingInt(StagesConsistencyItem::getPosition));
        logger.debug("Sort stagesConsistencyItemList by Position complete");

        // Prepare Map<StageId, StagesDescriptionItem>
        // По идее Редко искать → stream или for; Часто искать → Map. Но мне лень, поэтому Map.
        List<StagesDescriptionItem> stagesDescriptionItemList = pipeline.getStagesDescription();
        Map<String, StagesDescriptionItem> stageMap = stagesDescriptionItemList.stream()
                .collect(Collectors.toMap(StagesDescriptionItem::getStageId, Function.identity()));
        logger.debug("Prepare Map<StageId, StagesDescriptionItem> complete");

        // loop over stages
        for (StagesConsistencyItem stagesConsistencyItem : stagesConsistencyItemList) {
            if (! stageMap.containsKey(stagesConsistencyItem.getStageId())) throw new OperationIncompleteException("Stage "+ stagesConsistencyItem.getStageId() + " hasn't description");
            StagesDescriptionItem stagesDescriptionItem = stageMap.get(stagesConsistencyItem.getStageId());
            List<WorkersDescriptionItem> workersDescriptionItemList = stagesDescriptionItem.getWorkersDescription();


            // prepare stage workers' jobs
            for (WorkersDescriptionItem workersDescriptionItem : workersDescriptionItemList) {
                String workerName = workersDescriptionItem.getName();
                String workerDescription = workersDescriptionItem.getDescription();
                List<ConfigurationFilesItem> workerConfigurationFilesList = workersDescriptionItem.getConfigurationFiles(); // TODO: ConfigurationFile -> ConfigurationParameter

                List<ConfigurationParameter> configurationParameterList = new ArrayList<>();
                for (ConfigurationFilesItem configurationFilesItem : workerConfigurationFilesList) {
                    configurationParameterList.add(
                            ConfigurationParameter.newBuilder()
                                    // TODO: ConfigurationFile.getDescription() -> ConfigurationParameter.parameterKey()
                                    .parameterKey(configurationFilesItem.getDescription())
                                    // TODO: ConfigurationFile.getConfigurationFileId() -> ConfigurationParameter.parameterValue()
                                    .parameterValue(
                                            pathResolver.processTemplate(configurationFilesItem.getConfigurationFileId())
                                    )
                                    .build()
                    );
                }

                // uniqueId = node_name-pipeline_stage-module_name-uuid
                String uniqueId = nodeRepository.getNode(graphNodeRef).getName()
                        + "-" +
                        stagesDescriptionItem.getStageId()
                        + "-" +
                        workerName
                        + "-" +
                        UUID.randomUUID().toString().replace("-", "");

                HealthFlagProxy healthFlagProxy = HealthFlagProxy.newBuilder().build();

                WorkflowRoutineInterface workflowRoutine = moduleLoader.getModule(
                        workerName,
                        MCg3WorkflowRoutineApi.newBuilder()
                                .setStorageApi(
                                        new StorageApi(storageManager, graphNodeRef, nodeRepository)
                                )
                                .setConfigurationParameters(configurationParameterList)
                                .setExecutionContext(
                                        this.executionContextFactory.getExecutionContext(graphNodeRef, pathToCurrentExecutionNodeRef, stagesDescriptionItem, workersDescriptionItem, stagesConsistencyItem)
                                )
                                .build(),
                        healthFlagProxy
                );

                scheduler.addJob(
                        JobDetail.newBuilder()
                                .job(workflowRoutine)
                                .healthFlagProxy(healthFlagProxy)
                                .uniqueId(uniqueId)
                                .build()
                );


            }

            scheduler.runAllJobs(Boolean.TRUE);

            for (JobDetail jobDetail: scheduler.getJobDetails()) {
                if (scheduler.isJobActive(jobDetail)) throw new OperationIncompleteException("Job " + jobDetail + " has Active state after JoinThreads task completed");
                if (jobDetail.getHealthFlagProxy().getHealthFlagState() != HealthFlagState.COMPLETE) throw new OperationIncompleteException("Job " + jobDetail + " is not in COMPLETE state");
            }

        }
    }
}
