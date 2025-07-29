package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management.GraphPathManagerInterface;
import io.github.byzatic.tessera.engine.application.commons.logging.MdcWorkflowRoutineContext;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesConsistencyItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.WorkersDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeProjectGlobalRepositoryInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionContextFactory implements ExecutionContextFactoryInterface {

    private final JpaLikeNodeRepositoryInterface nodeRepository;
    private final JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository;
    private final Map<GraphNodeRef, ExecutionContextInterface> executionContextInterfaceMap = new HashMap<>();
    private final JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository;
    private final GraphPathManagerInterface graphPathManager;

    public ExecutionContextFactory(JpaLikeNodeRepositoryInterface nodeRepository, JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository, JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository, GraphPathManagerInterface graphPathManager) {
        this.nodeRepository = nodeRepository;
        this.projectGlobalRepository = projectGlobalRepository;
        this.nodeGlobalRepository = nodeGlobalRepository;
        this.graphPathManager = graphPathManager;
    }

    @Override
    public synchronized ExecutionContextInterface getExecutionContext(GraphNodeRef graphNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, StagesDescriptionItem stagesDescriptionItem, WorkersDescriptionItem workersDescriptionItem, StagesConsistencyItem stagesConsistencyItem) throws OperationIncompleteException {
        try {
            ExecutionContextInterface result;
            if (executionContextInterfaceMap.containsKey(graphNodeRef)) {
                result = executionContextInterfaceMap.get(graphNodeRef);
            } else {
                result = create(graphNodeRef, pathToCurrentExecutionNodeRef, stagesDescriptionItem,  workersDescriptionItem, stagesConsistencyItem);
            }
            return result;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private List<GraphPathInterface> getGraphPathList(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        List<GraphPathInterface> result = new ArrayList<>();
        for (String pathsString : this.graphPathManager.getRootPathsAsString(graphNodeRef, ".")) {
            result.add(GraphPath.newBuilder().setGraphPath(pathsString).build());
        }
        return result;
    }

    private List<NodeItem> convertNodeRefPathToNodeItemCompressedPath(List<GraphNodeRef> pathToCurrentExecutionNodeRef) throws OperationIncompleteException {
        try {
            List<NodeItem> nodeItemList = new ArrayList<>();
            for (GraphNodeRef graphNodeRef : pathToCurrentExecutionNodeRef) {
                nodeItemList.add(this.nodeRepository.getNode(graphNodeRef));
            }
            return nodeItemList;
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e);
        }
    }

    private ExecutionContextInterface create(GraphNodeRef graphNodeRef, List<GraphNodeRef> pathToCurrentExecutionNodeRef, StagesDescriptionItem stagesDescriptionItem, WorkersDescriptionItem workersDescriptionItem, StagesConsistencyItem stagesConsistencyItem) throws OperationIncompleteException {
        try {
            ExecutionContextInterface result;

            NodeItem nodeItem = this.nodeRepository.getNode(graphNodeRef);

            List<StorageDescriptionInterface> nodeStorageDescriptionList = new ArrayList<>();
            for (io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem storageItem : this.nodeGlobalRepository.getNodeGlobal(graphNodeRef).getStorages()) {
                nodeStorageDescriptionList.add(StorageDescription.newBuilder(storageItem).build());
            }

            NodeDescription nodeDescription = NodeDescription.newBuilder()
                    .setId(String.copyValueOf(nodeItem.getId().toCharArray()))
                    .setName(String.copyValueOf(nodeItem.getName().toCharArray()))
                    .setRootPaths(getGraphPathList(graphNodeRef))
                    .setStorageDescriptionList(nodeStorageDescriptionList)
                    .build();

            List<StorageDescriptionInterface> globalStorageDescriptionList = new ArrayList<>();
            for (StoragesItem storageItem : this.projectGlobalRepository.getProjectGlobal().getStorages()) {
                globalStorageDescriptionList.add(StorageDescription.newBuilder(storageItem).build());
            }

            PipelineDescription pipelineDescription = PipelineDescription.newBuilder()
                    .setStageName(String.copyValueOf(stagesDescriptionItem.getStageId().toCharArray()))
                    .build();

            List<NodeItem> path = convertNodeRefPathToNodeItemCompressedPath(pathToCurrentExecutionNodeRef);
            GraphPathInterface graphPath = GraphPath.newBuilder()
                    .setGraphPath(
                            path.stream()
                                    .map(NodeItem::getName)
                                    .collect(Collectors.joining("."))
                    )
                    .build();

            PipelineExecutionInfoInterface pipelineExecutionInfo = PipelineExecutionInfo.newBuilder()
                    .setCurrentNodeExecutionGraphPath(graphPath)
                    .build();

            MdcContextInterface mdcContext = MdcWorkflowRoutineContext.newBuilder()
                    .setNodeName(nodeItem.getName())
                    .setNodeIndex(nodeItem.getName())
                    .setStageName(stagesConsistencyItem.getStageId())
                    .setStageIndex(String.valueOf(stagesConsistencyItem.getPosition()))
                    .setRoutineName(workersDescriptionItem.getName())
                    .setRoutineIndex("Null")
                    .build();

            result = ExecutionContext.newBuilder()
                    .setNodeDescription(nodeDescription)
                    .setPipelineDescription(pipelineDescription)
                    .setGlobalStoragesDescription(globalStorageDescriptionList)
                    .setPipelineExecutionInfo(pipelineExecutionInfo)
                    .setMdcContext(mdcContext)
                    .build();

            executionContextInterfaceMap.put(graphNodeRef, result);

            return result;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public synchronized void reload() {
        executionContextInterfaceMap.clear();
    }



}
