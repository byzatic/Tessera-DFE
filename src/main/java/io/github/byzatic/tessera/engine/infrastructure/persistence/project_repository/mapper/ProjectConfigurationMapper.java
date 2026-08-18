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
