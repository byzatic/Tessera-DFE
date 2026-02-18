package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;

import java.util.*;

class GraphPathFinderIterative {

    private final FullProjectRepository fullProjectRepository;

    // --- cached index ---
    private volatile boolean indexReady = false;
    private final Object indexLock = new Object();

    // childUUID -> parents (GraphNodeRef)
    private final Map<String, List<GraphNodeRef>> parentsByChild = new HashMap<>();
    // sourceUUID set (indegree == 0)
    private final Set<String> sourceUuids = new HashSet<>();

    public GraphPathFinderIterative(FullProjectRepository fullProjectRepository) {
        this.fullProjectRepository = fullProjectRepository;
    }

    public List<List<NodeItem>> findAllPathsTo(GraphNodeRef targetGraphNodeRef) throws OperationIncompleteException {
        try {
            ensureIndex();

            NodeItem targetNode = fullProjectRepository.getNode(targetGraphNodeRef);
            if (targetNode == null) return List.of();

            String targetUuid = targetNode.getUUID();

            // memo: nodeUuid -> all paths (each path is list of nodeUuid from source..node)
            Map<String, List<List<String>>> memo = new HashMap<>();
            // recursion/loop guard (на случай некорректного графа)
            Set<String> inProgress = new HashSet<>();

            List<List<String>> uuidPaths = buildPathsTo(targetUuid, memo, inProgress);

            // convert UUID paths -> NodeItem paths
            List<List<NodeItem>> result = new ArrayList<>(uuidPaths.size());
            for (List<String> uuidPath : uuidPaths) {
                List<NodeItem> nodePath = new ArrayList<>(uuidPath.size());
                for (String uuid : uuidPath) {
                    // если у тебя нет конструктора GraphNodeRef(uuid) — замени на lookup uuid->GraphNodeRef (см. ниже)
                    NodeItem n = fullProjectRepository.getNode(GraphNodeRef.newBuilder().nodeUUID(uuid).build());
                    if (n != null) nodePath.add(n);
                }
                result.add(nodePath);
            }

            return result;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private void ensureIndex() throws OperationIncompleteException {
        if (indexReady) return;

        synchronized (indexLock) {
            if (indexReady) return;

            List<GraphNodeRef> allNodes = fullProjectRepository.listGraphNodeRef();

            // indegree by UUID
            Map<String, Integer> indegree = new HashMap<>(allNodes.size() * 2);

            // init
            for (GraphNodeRef ref : allNodes) {
                String uuid = ref.getNodeUUID();
                indegree.put(uuid, 0);
                parentsByChild.put(uuid, new ArrayList<>());
            }

            // fill reverse edges + indegree
            for (GraphNodeRef fromRef : allNodes) {
                NodeItem from = fullProjectRepository.getNode(fromRef);
                if (from == null) continue;

                for (GraphNodeRef toRef : from.getDownstream()) {
                    String toUuid = toRef.getNodeUUID();
                    parentsByChild.computeIfAbsent(toUuid, k -> new ArrayList<>()).add(fromRef);
                    indegree.merge(toUuid, 1, Integer::sum);
                }
            }

            // compute sources
            for (Map.Entry<String, Integer> e : indegree.entrySet()) {
                if (e.getValue() == 0) sourceUuids.add(e.getKey());
            }

            indexReady = true;
        }
    }

    private List<List<String>> buildPathsTo(
            String nodeUuid,
            Map<String, List<List<String>>> memo,
            Set<String> inProgress
    ) {
        List<List<String>> cached = memo.get(nodeUuid);
        if (cached != null) return cached;

        // cycle guard
        if (!inProgress.add(nodeUuid)) {
            return List.of(); // цикл — пути не считаем
        }

        List<List<String>> result = new ArrayList<>();

        if (sourceUuids.contains(nodeUuid)) {
            // base case: source -> itself
            result.add(new ArrayList<>(List.of(nodeUuid)));
        } else {
            List<GraphNodeRef> parents = parentsByChild.getOrDefault(nodeUuid, List.of());
            for (GraphNodeRef pRef : parents) {
                String pUuid = pRef.getNodeUUID();
                List<List<String>> parentPaths = buildPathsTo(pUuid, memo, inProgress);

                for (List<String> pp : parentPaths) {
                    ArrayList<String> path = new ArrayList<>(pp.size() + 1);
                    path.addAll(pp);
                    path.add(nodeUuid);
                    result.add(path);
                }
            }
        }

        inProgress.remove(nodeUuid);
        memo.put(nodeUuid, result);
        return result;
    }
}