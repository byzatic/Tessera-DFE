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

import java.util.*;

public class GraphManagerNodeRepository implements GraphManagerNodeRepositoryInterface {

    private static final Logger logger = LoggerFactory.getLogger(GraphManagerNodeRepository.class);

    // main storage
    private Map<GraphNodeRef, Node> nodeRefNodeMap = new HashMap<>();

    // cached roots (GraphNodeRef with indegree == 0)
    private List<GraphNodeRef> listRoot = new ArrayList<>();

    // cached downstream resolved to Node objects (hot path optimisation)
    private final Map<GraphNodeRef, List<Node>> downstreamCache = new HashMap<>();

    public GraphManagerNodeRepository(@NotNull FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        try {
            logger.debug("Initialise GraphManagerNodeRepository");
            ObjectsUtils.requireNonNull(
                    fullProjectRepository,
                    new IllegalArgumentException(FullProjectRepository.class.getSimpleName() + " should be NotNull")
            );

            List<GraphNodeRef> graphNodeRefs = fullProjectRepository.listGraphNodeRef();
            logger.debug("List of all GraphNodeRef size is {}", graphNodeRefs.size());

            // build nodeRef -> Node map
            for (GraphNodeRef graphNodeRef : graphNodeRefs) {
                NodeItem nodeItem = fullProjectRepository.getNode(graphNodeRef);
                Node newNode = Node.newBuilder()
                        .setGraphNodeRef(graphNodeRef)
                        .setDownstream(nodeItem.getDownstream())
                        .build();

                nodeRefNodeMap.put(graphNodeRef, newNode);

                // keep debug very light; this line is expensive on large graphs if debug enabled
                if (logger.isDebugEnabled()) {
                    logger.debug("Node {} updated with {}", nodeItem.getUUID(), graphNodeRef);
                }
            }

            // roots + downstream cache
            createRootNodeListV2();
            rebuildDownstreamCache();

            logger.debug("Initialisation of GraphManagerNodeRepository complete");
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    public GraphManagerNodeRepository(@NotNull Map<GraphNodeRef, Node> nodeRefNodeMap) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(nodeRefNodeMap, new IllegalArgumentException("Map<GraphNodeRef, Node> should be NotNull"));
            this.nodeRefNodeMap = nodeRefNodeMap;

            createRootNodeListV2();
            rebuildDownstreamCache();
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @NotNull
    @Override
    public synchronized Node getNode(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Request get Node by {}", graphNodeRef);
        ObjectsUtils.requireNonNull(graphNodeRef, new IllegalArgumentException(GraphNodeRef.class.getSimpleName() + " should be NotNull"));

        Node node = nodeRefNodeMap.get(graphNodeRef);
        if (node == null) {
            throw new OperationIncompleteException("No such Node was found by " + graphNodeRef);
        }

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

    /**
     * HOT PATH.
     * Returns resolved downstream nodes from cache.
     * Keeps synchronized as requested.
     */
    @NotNull
    @Override
    public synchronized List<Node> getNodeDownstream(@NotNull Node node) throws OperationIncompleteException {
        // NOTE: avoid containsValue(node) - it's O(n) and kills CPU on large graphs.
        ObjectsUtils.requireNonNull(node, new IllegalArgumentException(Node.class.getSimpleName() + " should be NotNull"));

        GraphNodeRef ref = node.getGraphNodeRef();
        List<Node> cached = downstreamCache.get(ref);

        if (cached == null) {
            // fallback (should not happen in normal flow)
            Node fromMap = nodeRefNodeMap.get(ref);
            if (fromMap == null) {
                throw new OperationIncompleteException("No such Node was found by " + node);
            }
            return List.of();
        }

        return cached;
    }

    @NotNull
    @Override
    public synchronized List<Node> getNodeDownstream(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Request get List downstream for jpa_like_node_repository by GraphNodeRef {}", graphNodeRef);
        ObjectsUtils.requireNonNull(graphNodeRef, new IllegalArgumentException(GraphNodeRef.class.getSimpleName() + " should be NotNull"));

        if (!nodeRefNodeMap.containsKey(graphNodeRef)) {
            throw new OperationIncompleteException("No such Node was found by " + graphNodeRef);
        }

        List<Node> cached = downstreamCache.get(graphNodeRef);
        return cached != null ? cached : List.of();
    }

    private void createRootNodeList() {
        // Legacy O(n²) method (kept for compatibility).
        logger.debug("Searching for Roots (legacy)");
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

    private void createRootNodeListV2() {
        // O(n) roots calculation.
        logger.debug("Searching for Roots");

        Set<GraphNodeRef> allDownstream = new HashSet<>();

        for (Node node : nodeRefNodeMap.values()) {
            allDownstream.addAll(node.getDownstream());
        }

        List<GraphNodeRef> roots = new ArrayList<>();

        for (GraphNodeRef ref : nodeRefNodeMap.keySet()) {
            if (!allDownstream.contains(ref)) {
                roots.add(ref);
            }
        }

        logger.debug("Searching for Roots complete; listAllRoot size is {}", roots.size());
        listRoot = roots;
    }

    /**
     * Builds cache: GraphNodeRef -> resolved downstream Node list.
     * This removes repeated allocations in getNodeDownstream().
     */
    private void rebuildDownstreamCache() {
        downstreamCache.clear();

        for (Map.Entry<GraphNodeRef, Node> e : nodeRefNodeMap.entrySet()) {
            GraphNodeRef ref = e.getKey();
            Node node = e.getValue();

            List<GraphNodeRef> downstreamRefs = node.getDownstream();
            if (downstreamRefs == null || downstreamRefs.isEmpty()) {
                downstreamCache.put(ref, List.of());
                continue;
            }

            List<Node> resolved = new ArrayList<>(downstreamRefs.size());
            for (GraphNodeRef childRef : downstreamRefs) {
                Node child = nodeRefNodeMap.get(childRef);
                if (child != null) {
                    resolved.add(child);
                }
            }

            downstreamCache.put(ref, Collections.unmodifiableList(resolved));
        }
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
        // leaving unsynchronized as in original code
        for (Map.Entry<GraphNodeRef, Node> nodeEntry : nodeRefNodeMap.entrySet()) {
            Node node = nodeEntry.getValue();
            node.setNodeLifecycleState(NodeLifecycleState.NOTSTATED);
        }
    }
}