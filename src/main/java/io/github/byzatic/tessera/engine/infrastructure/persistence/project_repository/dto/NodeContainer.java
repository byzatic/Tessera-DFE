package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeContainer {

    private final Map<GraphNodeRef, NodeGlobal> nodeGlobalMap;
    private final Map<GraphNodeRef, NodePipeline> nodePipelineMap;
    private final Map<GraphNodeRef, NodeItem> nodeMap;
    private final List<GraphNodeRef> graphNodeRefList;

    public NodeContainer(Map<GraphNodeRef, NodeItem> nodeMap, Map<GraphNodeRef, NodeGlobal> nodeGlobalMap, Map<GraphNodeRef, NodePipeline> nodePipelineMap) {
        this.nodeMap= new HashMap<>(nodeMap);
        this.graphNodeRefList = new ArrayList<>(nodeMap.keySet());
        this.nodeGlobalMap = nodeGlobalMap;
        this.nodePipelineMap = nodePipelineMap;
    }

    public List<GraphNodeRef> listGraphNodeRef() {
        return graphNodeRefList;
    }

    public NodeItem getNode(GraphNodeRef graphNodeRef) {
        NodeItem nodeItem = null;
        if (nodeMap.containsKey(graphNodeRef)) {
            nodeItem = nodeMap.get(graphNodeRef);
        } else {
            throw new IllegalArgumentException();
        }
        return nodeItem;
    }

    public Map<GraphNodeRef, NodeItem> getNodeMap() {
        return nodeMap;
    }


    public NodeGlobal getNodeGlobal(GraphNodeRef graphNodeRef) {
        NodeGlobal nodeGlobal = null;
        if (nodeGlobalMap.containsKey(graphNodeRef)) {
            nodeGlobal = nodeGlobalMap.get(graphNodeRef);
        } else {
            throw new IllegalArgumentException();
        }
        return nodeGlobal;
    }

    public Map<GraphNodeRef, NodeGlobal> getNodeGlobalMap() {
        return nodeGlobalMap;
    }

    public NodePipeline getNodePipeline(GraphNodeRef graphNodeRef) {
        NodePipeline nodePipeline = null;
        if (nodePipelineMap.containsKey(graphNodeRef)) {
            nodePipeline = nodePipelineMap.get(graphNodeRef);
        } else {
            throw new IllegalArgumentException();
        }
        return nodePipeline;
    }

    public Map<GraphNodeRef, NodePipeline> getNodePipelineMap() {
        return nodePipelineMap;
    }
}
