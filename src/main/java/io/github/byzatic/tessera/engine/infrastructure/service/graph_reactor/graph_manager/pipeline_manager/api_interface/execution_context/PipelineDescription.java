package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.workflowroutine.execution_context.PipelineDescriptionInterface;

import java.util.Objects;

public class PipelineDescription implements PipelineDescriptionInterface {
    private String stageName;

    public PipelineDescription() {
    }

    private PipelineDescription(Builder builder) {
        stageName = builder.stageName;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PipelineDescription copy) {
        Builder builder = new Builder();
        builder.stageName = copy.getStageName();
        return builder;
    }

    @Override
    public String getStageName() {
        return stageName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineDescription that = (PipelineDescription) o;
        return Objects.equals(stageName, that.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageName);
    }

    @Override
    public String toString() {
        return "PipelineDescription{" +
                "stageName='" + stageName + '\'' +
                '}';
    }

    /**
     * {@code PipelineDescription} builder static inner class.
     */
    public static final class Builder {
        private String stageName;

        private Builder() {
        }

        /**
         * Sets the {@code stageName} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param stageName the {@code stageName} to set
         * @return a reference to this Builder
         */
        public Builder setStageName(String stageName) {
            this.stageName = stageName;
            return this;
        }

        /**
         * Returns a {@code PipelineDescription} built from the parameters previously set.
         *
         * @return a {@code PipelineDescription} built with parameters of this {@code PipelineDescription.Builder}
         */
        public PipelineDescription build() {
            return new PipelineDescription(this);
        }
    }
}
