package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper;

import io.github.byzatic.tessera.lib.configio.unified.model.ConfigurationFile;
import io.github.byzatic.tessera.lib.configio.unified.model.NodeConfiguration;
import io.github.byzatic.tessera.lib.configio.unified.model.NodeId;
import io.github.byzatic.tessera.lib.configio.unified.model.PipelineStage;
import io.github.byzatic.tessera.lib.configio.unified.model.ProjectConfiguration;
import io.github.byzatic.tessera.lib.configio.unified.model.ProjectNode;
import io.github.byzatic.tessera.lib.configio.unified.model.ServiceDefinition;
import io.github.byzatic.tessera.lib.configio.unified.model.ServiceOption;
import io.github.byzatic.tessera.lib.configio.unified.model.StorageDefinition;
import io.github.byzatic.tessera.lib.configio.unified.model.StorageOption;
import io.github.byzatic.tessera.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.tessera.lib.configio.unified.model.Worker;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectConfigurationMapper {

    public ProjectRepositoryStateDataObject map(TesseraProject source) {
        NodeContainer nodeContainer = mapNodeContainer(source);
        GlobalContainer globalContainer = new GlobalContainer(
                mapGlobal(source.getConfiguration())
        );
        return new ProjectRepositoryStateDataObject(
                globalContainer,
                nodeContainer
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
            io.github.byzatic.tessera.lib.configio.unified.model.Pipeline source
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

}
