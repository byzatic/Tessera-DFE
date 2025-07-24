package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.dto.ConfigNodeItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.dto.ConfigProject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class SupportNodesStructureCompressor {
    private final static Logger logger = LoggerFactory.getLogger(SupportNodesStructureCompressor.class);
    private String namingTag = "#NAMED";

    public Map<GraphNodeRef, NodeItem> uncompress(ConfigProject configProject) throws OperationIncompleteException {
        try {
            Map<GraphNodeRef, NodeItem> nodeMap = new HashMap<>();
            nodeMap = collapseGraphStructure(configProject.getStructure());
            return nodeMap;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    // Flatten graph and replace downstream items with GraphNodeRef
    private Map<GraphNodeRef, NodeItem> collapseGraphStructure(ConfigNodeItem nodeItem) throws OperationIncompleteException {
        Map<GraphNodeRef, NodeItem> nodes = new HashMap<>();
        collapseGraphStructureProcessor(nodeItem, null, nodes);
        return nodes;
    }

    private void collapseGraphStructureProcessor(ConfigNodeItem nodeItem, GraphNodeRef graphNodeRef, Map<GraphNodeRef, NodeItem> nodes) throws OperationIncompleteException {
        String nodeUUUID = generateNodeUUUID(nodeItem);

        Map<ConfigNodeItem, GraphNodeRef> nodeDownstreamMap = new HashMap<>();
        for (ConfigNodeItem nodeDownstreamItem : nodeItem.getDownstream()) {
            GraphNodeRef downstreamGraphNodeRef = GraphNodeRef.newBuilder().nodeUUID(generateNodeUUUID(nodeDownstreamItem)).build();
            nodeDownstreamMap.put(nodeDownstreamItem, downstreamGraphNodeRef);
        }

        NodeItem node = NodeItem.newBuilder()
                .setUUID(nodeUUUID)
                .setId(nodeItem.getId())
                .setName(nodeItem.getName())
                .setDescription(nodeItem.getDescription())
                .setDownstream(nodeDownstreamMap.values().stream().toList())
                .build();

        if (graphNodeRef ==  null) {
            graphNodeRef = GraphNodeRef.newBuilder().nodeUUID(nodeUUUID).build();
        }
        nodes.put(graphNodeRef, node);

        for (Map.Entry<ConfigNodeItem, GraphNodeRef> nodeDownstreamEntry : nodeDownstreamMap.entrySet()) {
            collapseGraphStructureProcessor(nodeDownstreamEntry.getKey(), nodeDownstreamEntry.getValue(), nodes);
        }
    }

    private String generateNodeUUUID(ConfigNodeItem nodeItem) throws OperationIncompleteException {
        String nodeUuid;
        String uuid = String.valueOf(UUID.randomUUID());
        String namingTag = "#NAMED";
        if (nodeItem.getId().equals(namingTag)) {
            nodeUuid = uuid + "-" + namingTag + "-" + nodeItem.getName();
        } else {
            nodeUuid = uuid + "-" + nodeItem.getId() + "-" + nodeItem.getName();
        }
        return nodeUuid;
    }
}
