package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.GraphPathInterface;

import java.util.Objects;

public class GraphPath implements GraphPathInterface {
    private String graphPath;

    public GraphPath() {
    }

    private GraphPath(Builder builder) {
        graphPath = builder.graphPath;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GraphPath copy) {
        Builder builder = new Builder();
        builder.graphPath = copy.getGraphPath();
        return builder;
    }

    @Override
    public String getGraphPath() {
        return graphPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphPath graphPath1 = (GraphPath) o;
        return Objects.equals(graphPath, graphPath1.graphPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphPath);
    }

    @Override
    public String toString() {
        return "GraphPath{" +
                "graphPath='" + graphPath + '\'' +
                '}';
    }

    /**
     * {@code GraphPath} builder static inner class.
     */
    public static final class Builder {
        private String graphPath;

        private Builder() {
        }

        /**
         * Sets the {@code graphPath} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param graphPath the {@code graphPath} to set
         * @return a reference to this Builder
         */
        public Builder setGraphPath(String graphPath) {
            this.graphPath = graphPath;
            return this;
        }

        /**
         * Returns a {@code GraphPath} built from the parameters previously set.
         *
         * @return a {@code GraphPath} built with parameters of this {@code GraphPath.Builder}
         */
        public GraphPath build() {
            return new GraphPath(this);
        }
    }
}
