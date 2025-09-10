package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.common;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeToGNRContainer {
    private final Map<GraphNodeRef, NodeItem> graphNodeRefNodeItemMap;

    public NodeToGNRContainer(Map<GraphNodeRef, NodeItem> graphNodeRefNodeItemMap) {
        this.graphNodeRefNodeItemMap = graphNodeRefNodeItemMap;
    }
    
    public List<GraphNodeRef> getAllGraphNodeRef() {
        return new ArrayList<>(graphNodeRefNodeItemMap.keySet());
    }

    public NodeItem getNode(GraphNodeRef graphNodeRef) {
        if (graphNodeRefNodeItemMap.containsKey(graphNodeRef)) {
            return graphNodeRefNodeItemMap.get(graphNodeRef);
        } else {
            String errMessage = "Node with GraphNodeRef "+ graphNodeRef + " not exists";
            throw new RuntimeException(errMessage);
        }
    }

    public Boolean isGraphNodeRefExists(GraphNodeRef graphNodeRef) {
        return graphNodeRefNodeItemMap.containsKey(graphNodeRef);
    }
}
