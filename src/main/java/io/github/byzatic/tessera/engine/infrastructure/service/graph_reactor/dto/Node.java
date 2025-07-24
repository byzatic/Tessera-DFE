package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto;

import ru.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.NodeLifecycleState;

import java.util.List;
import java.util.Objects;

public class Node {
    private GraphNodeRef graphNodeRef = null;

    private NodeLifecycleState nodeLifecycleState = NodeLifecycleState.NOTSTATED;

    private List<GraphNodeRef> downstream = null;

    private Node() {
    }

    private Node(Builder builder) {
        ObjectsUtils.requireNonNull(builder.graphNodeRef, new IllegalArgumentException("graphNodeRef should be NotNull"));
        ObjectsUtils.requireNonNull(builder.downstream, new IllegalArgumentException("downstream should be NotNull"));
        graphNodeRef = builder.graphNodeRef;

        setNodeLifecycleState(builder.nodeLifecycleState);
        downstream = builder.downstream;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(Node copy) {
        Builder builder = new Builder();
        builder.graphNodeRef = copy.getGraphNodeRef();
        builder.nodeLifecycleState = copy.getNodeLifecycleState();
        builder.downstream = copy.getDownstream();
        return builder;
    }

    public GraphNodeRef getGraphNodeRef() {
        return graphNodeRef;
    }

    public synchronized NodeLifecycleState getNodeLifecycleState() {
        return nodeLifecycleState;
    }

    public synchronized void setNodeLifecycleState(NodeLifecycleState state) {
        this.nodeLifecycleState = state;
        if (state == NodeLifecycleState.READY) {
            this.notifyAll(); // Разбудить всех кто ждал на этой ноде
        }
    }

    public synchronized List<GraphNodeRef> getDownstream() {
        return downstream;
    }


    public synchronized void waitUntilReady() throws InterruptedException {
        while (nodeLifecycleState != NodeLifecycleState.READY) {
            this.wait(); // Ждать пока не станет READY
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(graphNodeRef, node.graphNodeRef) && nodeLifecycleState == node.nodeLifecycleState && Objects.equals(downstream, node.downstream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNodeRef, nodeLifecycleState, downstream);
    }

    @Override
    public String toString() {
        return "Node{" +
                "graphNodeRef=" + graphNodeRef +
                ", nodeLifecycleState=" + nodeLifecycleState +
                ", downstream=" + downstream +
                '}';
    }

    /**
     * {@code Node} builder static inner class.
     */
    public static final class Builder {
        private GraphNodeRef graphNodeRef = null;
        private NodeLifecycleState nodeLifecycleState = NodeLifecycleState.NOTSTATED;
        private List<GraphNodeRef> downstream = null;

        private Builder() {
        }

        /**
         * Sets the {@code graphNodeRef} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param graphNodeRef the {@code graphNodeRef} to set
         * @return a reference to this Builder
         */
        public Builder setGraphNodeRef(GraphNodeRef graphNodeRef) {
            this.graphNodeRef = graphNodeRef;
            return this;
        }

        /**
         * Sets the {@code nodeLifecycleState} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param nodeLifecycleState the {@code nodeLifecycleState} to set
         * @return a reference to this Builder
         */
        public Builder setNodeLifecycleState(NodeLifecycleState nodeLifecycleState) {
            this.nodeLifecycleState = nodeLifecycleState;
            return this;
        }

        /**
         * Sets the {@code downstream} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param downstream the {@code downstream} to set
         * @return a reference to this Builder
         */
        public Builder setDownstream(List<GraphNodeRef> downstream) {
            this.downstream = downstream;
            return this;
        }

        /**
         * Returns a {@code Node} built from the parameters previously set.
         *
         * @return a {@code Node} built with parameters of this {@code Node.Builder}
         */
        public Node build() {
            return new Node(this);
        }
    }
}
