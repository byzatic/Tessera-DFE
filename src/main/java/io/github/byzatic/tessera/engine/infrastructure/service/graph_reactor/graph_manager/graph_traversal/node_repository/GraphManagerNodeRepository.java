package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.NodeLifecycleState;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphManagerNodeRepository implements GraphManagerNodeRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(GraphManagerNodeRepository.class);
    Map<GraphNodeRef, Node> nodeRefNodeMap = new HashMap<>();

    List<GraphNodeRef> listRoot = new ArrayList<>();

    public GraphManagerNodeRepository(@NotNull FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        try {
            logger.debug("Initialise GraphManagerNodeRepository");
            ObjectsUtils.requireNonNull(fullProjectRepository, new IllegalArgumentException(FullProjectRepository.class.getSimpleName() + " should be NotNull"));
            List<GraphNodeRef> graphNodeRefs = fullProjectRepository.listGraphNodeRef();
            logger.debug("List of all GraphNodeRef size is {}", graphNodeRefs.size());
            for (GraphNodeRef graphNodeRef : graphNodeRefs) {
                NodeItem nodeItem = fullProjectRepository.getNode(graphNodeRef);
                Node newNode = Node.newBuilder()
                        .setGraphNodeRef(graphNodeRef)
                        .setDownstream(nodeItem.getDownstream())
                        .build();
                nodeRefNodeMap.put(graphNodeRef, newNode);
                createRootNodeList();
                logger.debug("Initialisation of GraphManagerNodeRepository complete");
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    public GraphManagerNodeRepository(@NotNull Map<GraphNodeRef, Node> nodeRefNodeMap) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(nodeRefNodeMap, new IllegalArgumentException("Map<GraphNodeRef, Node> should be NotNull"));
            this.nodeRefNodeMap = nodeRefNodeMap;
            createRootNodeList();
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @NotNull
    @Override
    public synchronized Node getNode(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Request get Node by {}", graphNodeRef);
        ObjectsUtils.requireNonNull(graphNodeRef, new IllegalArgumentException(GraphNodeRef.class.getSimpleName() + " should be NotNull"));
        if (!nodeRefNodeMap.containsKey(graphNodeRef))
            throw new OperationIncompleteException("No such Node was found by " + graphNodeRef);
        Node node = nodeRefNodeMap.get(graphNodeRef);
        logger.debug("Returns Node requested by {}", graphNodeRef);
        logger.trace("Returns Node {} requested by {}", node, graphNodeRef);
        return node;
    }

    @NotNull
    @Override
    public synchronized List<GraphNodeRef> listGraphNodeRef() {
        logger.debug("Request get List all GraphNodeRef");
        List<GraphNodeRef> graphNodeRefs = new ArrayList<>(nodeRefNodeMap.keySet());
        logger.debug("Returns List all GraphNodeRef of size {}", graphNodeRefs.size());
        logger.trace("Returns List all GraphNodeRef {}", graphNodeRefs);
        return graphNodeRefs;
    }

    @NotNull
    @Override
    public synchronized List<Node> getNodeDownstream(@NotNull Node node) throws OperationIncompleteException {
        logger.debug("Request get List downstream for jpa_like_node_repository {}", node);
        ObjectsUtils.requireNonNull(node, new IllegalArgumentException(Node.class.getSimpleName() + " should be NotNull"));
        if (!nodeRefNodeMap.containsValue(node))
            throw new OperationIncompleteException("No such Node was found by " + node);
        List<Node> downstream = new ArrayList<>();
        for (GraphNodeRef graphNodeRef : node.getDownstream()) {
            downstream.add(nodeRefNodeMap.get(graphNodeRef));
        }
        logger.debug("Returns List downstream of size {} for jpa_like_node_repository {}", downstream.size(), node);
        logger.trace("Returns List downstream {} for jpa_like_node_repository {}", downstream, node);
        return downstream;
    }

    @NotNull
    @Override
    public synchronized List<Node> getNodeDownstream(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Request get List downstream for jpa_like_node_repository by GraphNodeRef {}", graphNodeRef);
        ObjectsUtils.requireNonNull(graphNodeRef, new IllegalArgumentException(GraphNodeRef.class.getSimpleName() + " should be NotNull"));
        if (!nodeRefNodeMap.containsKey(graphNodeRef))
            throw new OperationIncompleteException("No such Node was found by " + graphNodeRef);
        Node node = nodeRefNodeMap.get(graphNodeRef);
        return getNodeDownstream(node);
    }

    private void createRootNodeList() {
        logger.debug("Searching for Roots");
        List<GraphNodeRef> listAllDownstream = new ArrayList<>();
        for (Map.Entry<GraphNodeRef, Node> nodeRefNodeSet : nodeRefNodeMap.entrySet()) {
            List<GraphNodeRef> downstream = nodeRefNodeSet.getValue().getDownstream();
            for (GraphNodeRef downstreamItem : downstream) {
                if (!listAllDownstream.contains(downstreamItem)) listAllDownstream.add(downstreamItem);
            }
        }
        List<GraphNodeRef> listAllRoot = new ArrayList<>();
        for (Map.Entry<GraphNodeRef, Node> nodeRefNodeSet : nodeRefNodeMap.entrySet()) {
            GraphNodeRef nodeRef = nodeRefNodeSet.getKey();
            if (!listAllDownstream.contains(nodeRef)) listAllRoot.add(nodeRef);
        }
        logger.debug("Searching for Roots complete; listAllRoot size is {}", listAllRoot.size());
        listRoot = listAllRoot;
    }

    @Override
    @NotNull
    public synchronized List<GraphNodeRef> getRootNodes() throws OperationIncompleteException {
        logger.debug("Request get List Root Nodes");
        List<GraphNodeRef> listAllRoot = listRoot;
        logger.debug("Returns List Root Nodes of size {}", listAllRoot.size());
        return listAllRoot;
    }

    @Override
    public @NotNull void clearNodeStatuses() throws OperationIncompleteException {
        for (Map.Entry<GraphNodeRef, Node> nodeEntry : nodeRefNodeMap.entrySet()) {
            Node node = nodeEntry.getValue();
            node.setNodeLifecycleState(NodeLifecycleState.NOTSTATED);
        }
    }

}
