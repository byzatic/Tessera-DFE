package io.github.byzatic.tessera.engine.graph_reactor.dto.graph_manager.node_repository;


import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.ProjectRepository;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.node_repository.GraphManagerNodeRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphManagerNodeRepositoryTest {

    private GraphNodeRef nodeRef1;
    private GraphNodeRef nodeRef2;
    private Node node1;
    private Node node2;

    private String genRndId() {
        return new Random().ints(8, 0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())
                .mapToObj(i -> "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(i))
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    @Before
    public void setUp() {
        nodeRef1 = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();
        nodeRef2 = GraphNodeRef.newBuilder().nodeUUID(genRndId()).build();

        node1 = Node.newBuilder()
                .setGraphNodeRef(nodeRef1)
                .setDownstream(Collections.singletonList(nodeRef2))
                .build();

        node2 = Node.newBuilder()
                .setGraphNodeRef(nodeRef2)
                .setDownstream(Collections.emptyList())
                .build();
    }

    @Test
    public void testConstructorFromNodeRepository() throws Exception {
        ProjectRepository projectRepository = mock(ProjectRepository.class);
        when(projectRepository.listGraphNodeRef()).thenReturn(Arrays.asList(nodeRef1, nodeRef2));

        NodeItem nic1 = mock(NodeItem.class);
        when(nic1.getDownstream()).thenReturn(Collections.singletonList(nodeRef2));

        NodeItem nic2 = mock(NodeItem.class);
        when(nic2.getDownstream()).thenReturn(Collections.emptyList());

        when(projectRepository.getNode(nodeRef1)).thenReturn(nic1);
        when(projectRepository.getNode(nodeRef2)).thenReturn(nic2);

        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(projectRepository);

        assertEquals(2, repo.listGraphNodeRef().size());
        assertEquals(Collections.singletonList(nodeRef2), repo.getNode(nodeRef1).getDownstream());
    }

    @Test
    public void testConstructorFromMap() throws Exception {
        Map<GraphNodeRef, Node> map = new HashMap<>();
        map.put(nodeRef1, node1);
        map.put(nodeRef2, node2);

        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(map);

        assertEquals(2, repo.listGraphNodeRef().size());
        assertEquals(node1, repo.getNode(nodeRef1));
    }

    @Test(expected = OperationIncompleteException.class)
    public void testGetNodeThrowsExceptionWhenMissing() throws Exception {
        Map<GraphNodeRef, Node> map = new HashMap<>();
        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(map);
        repo.getNode(nodeRef1); // should throw
    }

    @Test
    public void testGetNodeDownstreamFromNode() throws Exception {
        Map<GraphNodeRef, Node> map = new HashMap<>();
        map.put(nodeRef1, node1);
        map.put(nodeRef2, node2);

        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(map);
        List<Node> downstream = repo.getNodeDownstream(node1);
        assertEquals(1, downstream.size());
        assertEquals(node2, downstream.get(0));
    }

    @Test
    public void testGetNodeDownstreamFromRef() throws Exception {
        Map<GraphNodeRef, Node> map = new HashMap<>();
        map.put(nodeRef1, node1);
        map.put(nodeRef2, node2);

        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(map);
        List<Node> downstream = repo.getNodeDownstream(nodeRef1);
        assertEquals(1, downstream.size());
        assertEquals(node2, downstream.get(0));
    }

    @Test
    public void testListGraphNodeRef() throws Exception {
        Map<GraphNodeRef, Node> map = new HashMap<>();
        map.put(nodeRef1, node1);

        GraphManagerNodeRepository repo = new GraphManagerNodeRepository(map);
        List<GraphNodeRef> refs = repo.listGraphNodeRef();
        assertEquals(1, refs.size());
        assertTrue(refs.contains(nodeRef1));
    }
}