package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface;

import ru.byzatic.commons.ObjectsUtils;
import ru.byzatic.metrics_core.mcg3_storageapi_lib.storageapi.StorageApiInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.api_engine.MCg3WorkflowRoutineApiInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.configuration.ConfigurationParameter;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.ExecutionContextInterface;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MCg3WorkflowRoutineApi implements MCg3WorkflowRoutineApiInterface {
    private StorageApiInterface storageApi = null;
    private List<ConfigurationParameter> workflowRoutineConfigurationParameters = new LinkedList<>();
    private ExecutionContextInterface executionContext = null;

    private MCg3WorkflowRoutineApi() {
    }

    private MCg3WorkflowRoutineApi(Builder builder) {
        ObjectsUtils.requireNonNull(
                builder.storageApi,
                new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null " + StorageApiInterface.class.getSimpleName())
        );
        ObjectsUtils.requireNonNull(
                builder.workflowRoutineConfigurationParameters,
                new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null list of " + ConfigurationParameter.class.getSimpleName())
        );
        ObjectsUtils.requireNonNull(
                builder.executionContext,
                new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null " + ExecutionContextInterface.class.getSimpleName())
        );
        storageApi = builder.storageApi;
        workflowRoutineConfigurationParameters = builder.workflowRoutineConfigurationParameters;
        executionContext = builder.executionContext;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(MCg3WorkflowRoutineApi copy) {
        Builder builder = new Builder();
        builder.storageApi = copy.getStorageApi();
        builder.workflowRoutineConfigurationParameters = copy.getConfigurationParameters();
        builder.executionContext = copy.getExecutionContext();
        return builder;
    }

    @Override
    public StorageApiInterface getStorageApi() {
        return storageApi;
    }

    @Override
    public ExecutionContextInterface getExecutionContext() {
        return executionContext;
    }

    @Override
    public List<ConfigurationParameter> getConfigurationParameters() {
        return workflowRoutineConfigurationParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCg3WorkflowRoutineApi that = (MCg3WorkflowRoutineApi) o;
        return Objects.equals(storageApi, that.storageApi) && Objects.equals(workflowRoutineConfigurationParameters, that.workflowRoutineConfigurationParameters) && Objects.equals(executionContext, that.executionContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageApi, workflowRoutineConfigurationParameters, executionContext);
    }

    @Override
    public String toString() {
        return "MCg3WorkflowRoutineApi{" +
                "storageApi=" + storageApi +
                ", workflowRoutineConfigurationParameters=" + workflowRoutineConfigurationParameters +
                ", executionContext=" + executionContext +
                '}';
    }

    /**
     * {@code MCg3WorkflowRoutineApi} builder static inner class.
     */
    public static final class Builder {
        private StorageApiInterface storageApi;
        private List<ConfigurationParameter> workflowRoutineConfigurationParameters;
        private ExecutionContextInterface executionContext;

        private Builder() {
        }

        /**
         * Sets the {@code storageApi} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param storageApi the {@code storageApi} to set
         * @return a reference to this Builder
         */
        public Builder setStorageApi(StorageApiInterface storageApi) {
            this.storageApi = storageApi;
            return this;
        }

        /**
         * Sets the {@code workflowRoutineConfigurationParameters} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param workflowRoutineConfigurationParameters the {@code workflowRoutineConfigurationParameters} to set
         * @return a reference to this Builder
         */
        public Builder setConfigurationParameters(List<ConfigurationParameter> workflowRoutineConfigurationParameters) {
            this.workflowRoutineConfigurationParameters = workflowRoutineConfigurationParameters;
            return this;
        }

        /**
         * Sets the {@code executionContext} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param executionContext the {@code executionContext} to set
         * @return a reference to this Builder
         */
        public Builder setExecutionContext(ExecutionContextInterface executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        /**
         * Returns a {@code MCg3WorkflowRoutineApi} built from the parameters previously set.
         *
         * @return a {@code MCg3WorkflowRoutineApi} built with parameters of this {@code MCg3WorkflowRoutineApi.Builder}
         */
        public MCg3WorkflowRoutineApi build() {
            return new MCg3WorkflowRoutineApi(this);
        }
    }
}
