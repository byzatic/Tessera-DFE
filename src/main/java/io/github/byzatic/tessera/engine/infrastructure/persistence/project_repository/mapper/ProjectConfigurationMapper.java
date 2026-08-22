package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper;

import io.github.byzatic.lib.configio.domain.model.ConfigurationFileDataObject;
import io.github.byzatic.lib.configio.domain.model.ConfigurationOptionDataObject;
import io.github.byzatic.lib.configio.domain.model.GraphNodeReferenceDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeContainerDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeGlobalDataObject;
import io.github.byzatic.lib.configio.domain.model.PipelineDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectGlobalDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.domain.model.ServiceDataObject;
import io.github.byzatic.lib.configio.domain.model.StageConsistencyDataObject;
import io.github.byzatic.lib.configio.domain.model.StageDescriptionDataObject;
import io.github.byzatic.lib.configio.domain.model.StorageDataObject;
import io.github.byzatic.lib.configio.domain.model.WorkerDescriptionDataObject;
import io.github.byzatic.lib.configio.unified.model.ConfigurationFile;
import io.github.byzatic.lib.configio.unified.model.NodeConfiguration;
import io.github.byzatic.lib.configio.unified.model.NodeId;
import io.github.byzatic.lib.configio.unified.model.PipelineStage;
import io.github.byzatic.lib.configio.unified.model.ProjectConfiguration;
import io.github.byzatic.lib.configio.unified.model.ProjectNode;
import io.github.byzatic.lib.configio.unified.model.ServiceDefinition;
import io.github.byzatic.lib.configio.unified.model.ServiceOption;
import io.github.byzatic.lib.configio.unified.model.StorageDefinition;
import io.github.byzatic.lib.configio.unified.model.StorageOption;
import io.github.byzatic.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.lib.configio.unified.model.Worker;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_global.OptionsItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.ConfigurationFilesItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesConsistencyItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.WorkersDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.model.project.ServiceItem;
import io.github.byzatic.tessera.engine.domain.model.project.ServicesOptionsItem;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesOptionsItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectConfigurationMapper {

    public ProjectRepositoryStateDataObject map(ProjectLoadResultDataObject source) {
        NodeContainer nodeContainer = mapNodeContainer(source.getNodeContainer());
        GlobalContainer globalContainer = new GlobalContainer(mapGlobal(source.getGlobal()));
        SharedResourcesContainer sharedResourcesContainer = new SharedResourcesContainer(
                source.getSharedResourcesContainer().getClassLoaders()
        );
        return new ProjectRepositoryStateDataObject(
                globalContainer,
                nodeContainer,
                sharedResourcesContainer
        );
    }

    public ProjectRepositoryStateDataObject map(TesseraProject source) {
        NodeContainer nodeContainer = mapNodeContainer(source);
        GlobalContainer globalContainer = new GlobalContainer(
                mapGlobal(source.getConfiguration())
        );
        SharedResourcesContainer sharedResourcesContainer =
                new SharedResourcesContainer(List.<ClassLoader>of());
        return new ProjectRepositoryStateDataObject(
                globalContainer,
                nodeContainer,
                sharedResourcesContainer
        );
    }

    private NodeContainer mapNodeContainer(TesseraProject source) {
        Map<NodeId, GraphNodeRef> references = mapReferences(source);
        Map<GraphNodeRef, NodeItem> nodes = new LinkedHashMap<GraphNodeRef, NodeItem>();
        Map<GraphNodeRef, NodeGlobal> globals =
                new LinkedHashMap<GraphNodeRef, NodeGlobal>();
        Map<GraphNodeRef, NodePipeline> pipelines =
                new LinkedHashMap<GraphNodeRef, NodePipeline>();

        for (Map.Entry<NodeId, ProjectNode> entry : source.getNodes().entrySet()) {
            GraphNodeRef targetReference = references.get(entry.getKey());
            ProjectNode sourceNode = entry.getValue();
            nodes.put(targetReference, mapNode(sourceNode, references));
            globals.put(targetReference, mapNodeGlobal(sourceNode.getConfiguration()));
            pipelines.put(targetReference, mapPipeline(sourceNode.getPipeline()));
        }
        return new NodeContainer(nodes, globals, pipelines);
    }

    private Map<NodeId, GraphNodeRef> mapReferences(TesseraProject source) {
        Map<NodeId, GraphNodeRef> result = new LinkedHashMap<NodeId, GraphNodeRef>();
        for (NodeId nodeId : source.getNodes().keySet()) {
            result.put(nodeId, GraphNodeRef.newBuilder()
                    .nodeUUID(nodeId.getValue())
                    .build());
        }
        return result;
    }

    private NodeItem mapNode(
            ProjectNode source,
            Map<NodeId, GraphNodeRef> references
    ) {
        List<GraphNodeRef> downstream = new ArrayList<GraphNodeRef>();
        for (NodeId sourceReference : source.getDownstream()) {
            GraphNodeRef targetReference = references.get(sourceReference);
            if (targetReference == null) {
                throw new IllegalArgumentException(
                        "Unknown downstream node reference: " + sourceReference
                );
            }
            downstream.add(targetReference);
        }
        return NodeItem.newBuilder()
                .setUUID(source.getNodeId().getValue())
                .setId(source.getId())
                .setName(source.getName())
                .setDescription(source.getDescription())
                .setDownstream(downstream)
                .build();
    }

    private NodeGlobal mapNodeGlobal(NodeConfiguration source) {
        List<io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem>
                storages = new ArrayList<io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem>();
        for (StorageDefinition storage : source.getStorages()) {
            List<OptionsItem> options = new ArrayList<OptionsItem>();
            for (StorageOption option : storage.getOptions()) {
                options.add(OptionsItem.newBuilder()
                        .value(option.getValue())
                        .key(option.getKey())
                        .build());
            }
            storages.add(io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem
                    .newBuilder()
                    .options(options)
                    .description(storage.getDescription())
                    .idName(storage.getId())
                    .build());
        }
        return NodeGlobal.newBuilder().storages(storages).build();
    }

    private ProjectGlobal mapGlobal(ProjectConfiguration source) {
        List<io.github.byzatic.tessera.engine.domain.model.project.StoragesItem>
                storages = new ArrayList<io.github.byzatic.tessera.engine.domain.model.project.StoragesItem>();
        for (StorageDefinition storage : source.getStorages()) {
            List<StoragesOptionsItem> options = new ArrayList<StoragesOptionsItem>();
            for (StorageOption option : storage.getOptions()) {
                options.add(StoragesOptionsItem.newBuilder()
                        .data(option.getKey())
                        .name(option.getValue())
                        .build());
            }
            storages.add(io.github.byzatic.tessera.engine.domain.model.project.StoragesItem
                    .newBuilder()
                    .options(options)
                    .description(storage.getDescription())
                    .idName(storage.getId())
                    .build());
        }

        List<ServiceItem> services = new ArrayList<ServiceItem>();
        for (ServiceDefinition service : source.getServices()) {
            List<ServicesOptionsItem> options = new ArrayList<ServicesOptionsItem>();
            for (ServiceOption option : service.getOptions()) {
                options.add(ServicesOptionsItem.newBuilder()
                        .data(option.getData())
                        .name(option.getName())
                        .build());
            }
            services.add(ServiceItem.newBuilder()
                    .options(options)
                    .description(service.getDescription())
                    .idName(service.getId())
                    .build());
        }
        return ProjectGlobal.newBuilder().storages(storages).services(services).build();
    }

    private NodePipeline mapPipeline(
            io.github.byzatic.lib.configio.unified.model.Pipeline source
    ) {
        List<StagesConsistencyItem> consistency =
                new ArrayList<StagesConsistencyItem>();
        List<StagesDescriptionItem> descriptions =
                new ArrayList<StagesDescriptionItem>();
        for (PipelineStage stage : source.getStages()) {
            consistency.add(StagesConsistencyItem.newBuilder()
                    .stageId(stage.getId())
                    .position(stage.getPosition())
                    .build());
            descriptions.add(StagesDescriptionItem.newBuilder()
                    .workersDescription(mapUnifiedWorkers(stage.getWorkers()))
                    .stageId(stage.getId())
                    .build());
        }
        return NodePipeline.newBuilder()
                .stagesConsistency(consistency)
                .stagesDescription(descriptions)
                .build();
    }

    private List<WorkersDescriptionItem> mapUnifiedWorkers(List<Worker> source) {
        List<WorkersDescriptionItem> workers = new ArrayList<WorkersDescriptionItem>();
        for (Worker worker : source) {
            List<ConfigurationFilesItem> configurationFiles =
                    new ArrayList<ConfigurationFilesItem>();
            for (ConfigurationFile file : worker.getConfigurationFiles()) {
                configurationFiles.add(ConfigurationFilesItem.newBuilder()
                        .description(file.getDescription())
                        .configurationFileId(file.getId())
                        .build());
            }
            workers.add(WorkersDescriptionItem.newBuilder()
                    .name(worker.getName())
                    .description(worker.getDescription())
                    .configurationFiles(configurationFiles)
                    .build());
        }
        return workers;
    }

    private NodeContainer mapNodeContainer(NodeContainerDataObject source) {
        Map<GraphNodeReferenceDataObject, GraphNodeRef> references =
                mapReferences(source);
        Map<GraphNodeRef, NodeItem> nodes = new LinkedHashMap<GraphNodeRef, NodeItem>();
        Map<GraphNodeRef, NodeGlobal> globals =
                new LinkedHashMap<GraphNodeRef, NodeGlobal>();
        Map<GraphNodeRef, NodePipeline> pipelines =
                new LinkedHashMap<GraphNodeRef, NodePipeline>();

        for (GraphNodeReferenceDataObject sourceReference
                : source.getProjectStructure().getNodeReferences()) {
            GraphNodeRef targetReference = references.get(sourceReference);
            NodeDataObject sourceNode = source.getProjectStructure().getNode(sourceReference);
            nodes.put(targetReference, mapNode(sourceNode, references));
            globals.put(targetReference, mapNodeGlobal(source.getNodeGlobal(sourceReference)));
            pipelines.put(targetReference, mapPipeline(source.getPipeline(sourceReference)));
        }
        return new NodeContainer(nodes, globals, pipelines);
    }

    private Map<GraphNodeReferenceDataObject, GraphNodeRef> mapReferences(
            NodeContainerDataObject source
    ) {
        Map<GraphNodeReferenceDataObject, GraphNodeRef> result =
                new LinkedHashMap<GraphNodeReferenceDataObject, GraphNodeRef>();
        for (GraphNodeReferenceDataObject reference
                : source.getProjectStructure().getNodeReferences()) {
            result.put(reference, GraphNodeRef.newBuilder()
                    .nodeUUID(reference.getNodeUuid())
                    .build());
        }
        return result;
    }

    private NodeItem mapNode(
            NodeDataObject source,
            Map<GraphNodeReferenceDataObject, GraphNodeRef> references
    ) {
        List<GraphNodeRef> downstream = new ArrayList<GraphNodeRef>();
        for (GraphNodeReferenceDataObject sourceReference : source.getDownstream()) {
            GraphNodeRef targetReference = references.get(sourceReference);
            if (targetReference == null) {
                throw new IllegalArgumentException(
                        "Unknown downstream node reference: " + sourceReference
                );
            }
            downstream.add(targetReference);
        }
        return NodeItem.newBuilder()
                .setUUID(source.getUuid())
                .setId(source.getId())
                .setName(source.getName())
                .setDescription(source.getDescription())
                .setDownstream(downstream)
                .build();
    }

    private NodeGlobal mapNodeGlobal(NodeGlobalDataObject source) {
        List<io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem>
                storages = new ArrayList<io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem>();
        for (StorageDataObject storage : source.getStorages()) {
            List<OptionsItem> options = new ArrayList<OptionsItem>();
            for (ConfigurationOptionDataObject option : storage.getOptions()) {
                options.add(OptionsItem.newBuilder()
                        .value(option.getValue())
                        .key(option.getKey())
                        .build());
            }
            storages.add(io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem
                    .newBuilder()
                    .options(options)
                    .description(storage.getDescription())
                    .idName(storage.getIdName())
                    .build());
        }
        return NodeGlobal.newBuilder().storages(storages).build();
    }

    private ProjectGlobal mapGlobal(ProjectGlobalDataObject source) {
        List<io.github.byzatic.tessera.engine.domain.model.project.StoragesItem>
                storages = new ArrayList<io.github.byzatic.tessera.engine.domain.model.project.StoragesItem>();
        for (StorageDataObject storage : source.getStorages()) {
            List<StoragesOptionsItem> options = new ArrayList<StoragesOptionsItem>();
            for (ConfigurationOptionDataObject option : storage.getOptions()) {
                options.add(StoragesOptionsItem.newBuilder()
                        .data(option.getKey())
                        .name(option.getValue())
                        .build());
            }
            storages.add(io.github.byzatic.tessera.engine.domain.model.project.StoragesItem
                    .newBuilder()
                    .options(options)
                    .description(storage.getDescription())
                    .idName(storage.getIdName())
                    .build());
        }

        List<ServiceItem> services = new ArrayList<ServiceItem>();
        for (ServiceDataObject service : source.getServices()) {
            List<ServicesOptionsItem> options = new ArrayList<ServicesOptionsItem>();
            for (ConfigurationOptionDataObject option : service.getOptions()) {
                options.add(ServicesOptionsItem.newBuilder()
                        .data(option.getData())
                        .name(option.getName())
                        .build());
            }
            services.add(ServiceItem.newBuilder()
                    .options(options)
                    .description(service.getDescription())
                    .idName(service.getIdName())
                    .build());
        }
        return ProjectGlobal.newBuilder().storages(storages).services(services).build();
    }

    private NodePipeline mapPipeline(PipelineDataObject source) {
        List<StagesConsistencyItem> consistency =
                new ArrayList<StagesConsistencyItem>();
        for (StageConsistencyDataObject stage : source.getStagesConsistency()) {
            consistency.add(StagesConsistencyItem.newBuilder()
                    .stageId(stage.getStageId())
                    .position(stage.getPosition())
                    .build());
        }

        List<StagesDescriptionItem> descriptions =
                new ArrayList<StagesDescriptionItem>();
        for (StageDescriptionDataObject stage : source.getStagesDescription()) {
            descriptions.add(StagesDescriptionItem.newBuilder()
                    .workersDescription(mapWorkers(stage.getWorkers()))
                    .stageId(stage.getStageId())
                    .build());
        }
        return NodePipeline.newBuilder()
                .stagesConsistency(consistency)
                .stagesDescription(descriptions)
                .build();
    }

    private List<WorkersDescriptionItem> mapWorkers(
            List<WorkerDescriptionDataObject> source
    ) {
        List<WorkersDescriptionItem> workers = new ArrayList<WorkersDescriptionItem>();
        for (WorkerDescriptionDataObject worker : source) {
            List<ConfigurationFilesItem> configurationFiles =
                    new ArrayList<ConfigurationFilesItem>();
            for (ConfigurationFileDataObject file : worker.getConfigurationFiles()) {
                configurationFiles.add(ConfigurationFilesItem.newBuilder()
                        .description(file.getDescription())
                        .configurationFileId(file.getConfigurationFileId())
                        .build());
            }
            workers.add(WorkersDescriptionItem.newBuilder()
                    .name(worker.getName())
                    .description(worker.getDescription())
                    .configurationFiles(configurationFiles)
                    .build());
        }
        return workers;
    }
}
