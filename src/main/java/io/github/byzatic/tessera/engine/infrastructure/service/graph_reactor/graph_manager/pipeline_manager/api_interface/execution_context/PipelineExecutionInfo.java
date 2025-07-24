package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.GraphPathInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.PipelineExecutionInfoInterface;

import java.util.Objects;

public class PipelineExecutionInfo implements PipelineExecutionInfoInterface {
    private GraphPathInterface currentNodeExecutionGraphPath;

    public PipelineExecutionInfo() {
    }

    private PipelineExecutionInfo(Builder builder) {
        currentNodeExecutionGraphPath = builder.currentNodeExecutionGraphPath;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PipelineExecutionInfo copy) {
        Builder builder = new Builder();
        builder.currentNodeExecutionGraphPath = copy.getCurrentNodeExecutionGraphPath();
        return builder;
    }

    @Override
    public GraphPathInterface getCurrentNodeExecutionGraphPath() {
        return currentNodeExecutionGraphPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineExecutionInfo that = (PipelineExecutionInfo) o;
        return Objects.equals(currentNodeExecutionGraphPath, that.currentNodeExecutionGraphPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentNodeExecutionGraphPath);
    }

    @Override
    public String toString() {
        return "PipelineExecutionInfo{" +
                "currentNodeExecutionGraphPath=" + currentNodeExecutionGraphPath +
                '}';
    }

    /**
     * {@code PipelineExecutionInfo} builder static inner class.
     */
    public static final class Builder {
        private GraphPathInterface currentNodeExecutionGraphPath;

        private Builder() {
        }

        /**
         * Sets the {@code currentNodeExecutionGraphPath} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param currentNodeExecutionGraphPath the {@code currentNodeExecutionGraphPath} to set
         * @return a reference to this Builder
         */
        public Builder setCurrentNodeExecutionGraphPath(GraphPathInterface currentNodeExecutionGraphPath) {
            this.currentNodeExecutionGraphPath = currentNodeExecutionGraphPath;
            return this;
        }

        /**
         * Returns a {@code PipelineExecutionInfo} built from the parameters previously set.
         *
         * @return a {@code PipelineExecutionInfo} built with parameters of this {@code PipelineExecutionInfo.Builder}
         */
        public PipelineExecutionInfo build() {
            return new PipelineExecutionInfo(this);
        }
    }
}
