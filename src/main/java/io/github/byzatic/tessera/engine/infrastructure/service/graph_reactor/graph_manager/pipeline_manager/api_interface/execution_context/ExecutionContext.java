package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import ru.byzatic.commons.ObjectsUtils;
import ru.byzatic.metrics_core.mcg3_enginecommon_lib.logging.MdcContextInterface;
import ru.byzatic.metrics_core.mcg3_storageapi_lib.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.*;

import java.util.List;
import java.util.Objects;

public class ExecutionContext implements ExecutionContextInterface {
    private PipelineDescriptionInterface pipelineDescription;
    private NodeDescriptionInterface nodeDescription;
    private List<StorageDescriptionInterface> globalStoragesDescription;

    private PipelineExecutionInfoInterface pipelineExecutionInfo;
    private MdcContextInterface mdcContext;

    public ExecutionContext() {
    }

    private ExecutionContext(Builder builder) {
        ObjectsUtils.requireNonNull(builder.pipelineDescription, new IllegalArgumentException("pipelineDescription must be not null"));
        ObjectsUtils.requireNonNull(builder.nodeDescription, new IllegalArgumentException("nodeDescription must be not null"));
        ObjectsUtils.requireNonNull(builder.globalStoragesDescription, new IllegalArgumentException("globalStoragesDescription must be not null"));
        ObjectsUtils.requireNonNull(builder.pipelineExecutionInfo, new IllegalArgumentException("pipelineExecutionInfo must be not null"));
        ObjectsUtils.requireNonNull(builder.mdcContext, new IllegalArgumentException("mdcContext must be not null"));
        pipelineDescription = builder.pipelineDescription;
        nodeDescription = builder.nodeDescription;
        globalStoragesDescription = builder.globalStoragesDescription;
        pipelineExecutionInfo = builder.pipelineExecutionInfo;
        mdcContext = builder.mdcContext;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ExecutionContext copy) throws OperationIncompleteException {
        try {
            Builder builder = new Builder();
            builder.pipelineDescription = copy.getPipelineDescription();
            builder.nodeDescription = copy.getNodeDescription();
            builder.globalStoragesDescription = copy.getGlobalStoragesDescription();
            builder.pipelineExecutionInfo = copy.getPipelineExecutionInfo();
            builder.mdcContext = copy.getMdcContext();
            return builder;
        } catch (MCg3ApiOperationIncompleteException e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public PipelineDescriptionInterface getPipelineDescription() throws MCg3ApiOperationIncompleteException {
        return pipelineDescription;
    }

    @Override
    public NodeDescriptionInterface getNodeDescription() throws MCg3ApiOperationIncompleteException {
        return nodeDescription;
    }

    @Override
    public List<StorageDescriptionInterface> getGlobalStoragesDescription() throws MCg3ApiOperationIncompleteException {
        return globalStoragesDescription;
    }

    @Override
    public PipelineExecutionInfoInterface getPipelineExecutionInfo() throws MCg3ApiOperationIncompleteException {
        return pipelineExecutionInfo;
    }

    @Override
    public MdcContextInterface getMdcContext() {
        return mdcContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionContext that = (ExecutionContext) o;
        return Objects.equals(pipelineDescription, that.pipelineDescription) && Objects.equals(nodeDescription, that.nodeDescription) && Objects.equals(globalStoragesDescription, that.globalStoragesDescription) && Objects.equals(pipelineExecutionInfo, that.pipelineExecutionInfo) && Objects.equals(mdcContext, that.mdcContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineDescription, nodeDescription, globalStoragesDescription, pipelineExecutionInfo, mdcContext);
    }

    @Override
    public String toString() {
        return "ExecutionContext{" +
                "pipelineDescription=" + pipelineDescription +
                ", nodeDescription=" + nodeDescription +
                ", globalStoragesDescription=" + globalStoragesDescription +
                ", pipelineExecutionInfo=" + pipelineExecutionInfo +
                ", mdcContext=" + mdcContext +
                '}';
    }

    /**
     * {@code ExecutionContext} builder static inner class.
     */
    public static final class Builder {
        private PipelineDescriptionInterface pipelineDescription = null;
        private NodeDescriptionInterface nodeDescription = null;
        private List<StorageDescriptionInterface> globalStoragesDescription = null;
        private PipelineExecutionInfoInterface pipelineExecutionInfo = null;
        private MdcContextInterface mdcContext = null;

        private Builder() {
        }

        /**
         * Sets the {@code pipelineDescription} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param pipelineDescription the {@code pipelineDescription} to set
         * @return a reference to this Builder
         */
        public Builder setPipelineDescription(PipelineDescriptionInterface pipelineDescription) {
            this.pipelineDescription = pipelineDescription;
            return this;
        }

        /**
         * Sets the {@code nodeDescription} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param nodeDescription the {@code nodeDescription} to set
         * @return a reference to this Builder
         */
        public Builder setNodeDescription(NodeDescriptionInterface nodeDescription) {
            this.nodeDescription = nodeDescription;
            return this;
        }

        /**
         * Sets the {@code globalStoragesDescription} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param globalStoragesDescription the {@code globalStoragesDescription} to set
         * @return a reference to this Builder
         */
        public Builder setGlobalStoragesDescription(List<StorageDescriptionInterface> globalStoragesDescription) {
            this.globalStoragesDescription = globalStoragesDescription;
            return this;
        }

        /**
         * Sets the {@code pipelineExecutionInfo} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param pipelineExecutionInfo the {@code pipelineExecutionInfo} to set
         * @return a reference to this Builder
         */
        public Builder setPipelineExecutionInfo(PipelineExecutionInfoInterface pipelineExecutionInfo) {
            this.pipelineExecutionInfo = pipelineExecutionInfo;
            return this;
        }

        /**
         * Sets the {@code mdcContext} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param mdcContext the {@code mdcContext} to set
         * @return a reference to this Builder
         */
        public Builder setMdcContext(MdcContextInterface mdcContext) {
            this.mdcContext = mdcContext;
            return this;
        }

        /**
         * Returns a {@code ExecutionContext} built from the parameters previously set.
         *
         * @return a {@code ExecutionContext} built with parameters of this {@code ExecutionContext.Builder}
         */
        public ExecutionContext build() {
            return new ExecutionContext(this);
        }
    }
}
