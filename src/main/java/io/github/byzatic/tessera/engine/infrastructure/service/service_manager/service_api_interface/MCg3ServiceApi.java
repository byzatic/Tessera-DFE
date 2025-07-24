package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface;

import ru.byzatic.metrics_core.mcg3_storageapi_lib.storageapi.StorageApiInterface;
import ru.byzatic.metrics_core.service_lib.api_engine.MCg3ServiceApiInterface;
import ru.byzatic.metrics_core.service_lib.configuration.ServiceConfigurationParameter;
import ru.byzatic.metrics_core.service_lib.execution_context.ExecutionContextInterface;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class MCg3ServiceApi implements MCg3ServiceApiInterface {
    private StorageApiInterface storageApi = null;
    private ExecutionContextInterface executionContext = null;
    private List<ServiceConfigurationParameter> serviceConfigurationParameters = new LinkedList<>();

    private MCg3ServiceApi() {
    }

    private MCg3ServiceApi(Builder builder) {
        if (builder.storageApi == null) throw new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null " + StorageApiInterface.class.getSimpleName());
        if (builder.serviceConfigurationParameters == null) throw new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null " + StorageApiInterface.class.getSimpleName());
        if (builder.executionContext == null) throw new IllegalArgumentException("Can't create " + this.getClass().getSimpleName() + " with null " + StorageApiInterface.class.getSimpleName());
        storageApi = builder.storageApi;
        serviceConfigurationParameters = builder.serviceConfigurationParameters;
        executionContext = builder.executionContext;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(MCg3ServiceApi copy) {
        Builder builder = new Builder();
        builder.storageApi = copy.getStorageApi();
        builder.serviceConfigurationParameters = copy.getServiceConfigurationParameters();
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
    public List<ServiceConfigurationParameter> getServiceConfigurationParameters() {
        return serviceConfigurationParameters;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MCg3ServiceApi that = (MCg3ServiceApi) o;
        return Objects.equals(storageApi, that.storageApi) && Objects.equals(serviceConfigurationParameters, that.serviceConfigurationParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageApi, serviceConfigurationParameters);
    }

    @Override
    public String toString() {
        return "MCg3ServiceApi{" +
                "storageApi=" + storageApi +
                ", serviceConfigurationParameters=" + serviceConfigurationParameters +
                '}';
    }

    /**
     * {@code MCg3ServiceApi} builder static inner class.
     */
    public static final class Builder {
        private StorageApiInterface storageApi;
        private ExecutionContextInterface executionContext;
        private List<ServiceConfigurationParameter> serviceConfigurationParameters;

        private Builder() {
        }

        /**
         * Sets the {@code storageApi} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code storageApi} to set
         * @return a reference to this Builder
         */
        public Builder storageApi(StorageApiInterface val) {
            storageApi = val;
            return this;
        }

        /**
         * Sets the {@code storageApi} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code storageApi} to set
         * @return a reference to this Builder
         */
        public Builder executionContext(ExecutionContextInterface val) {
            executionContext = val;
            return this;
        }

        /**
         * Sets the {@code serviceConfigurationParameters} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code serviceConfigurationParameters} to set
         * @return a reference to this Builder
         */
        public Builder serviceConfigurationParameters(List<ServiceConfigurationParameter> val) {
            serviceConfigurationParameters = val;
            return this;
        }

        /**
         * Returns a {@code MCg3ServiceApi} built from the parameters previously set.
         *
         * @return a {@code MCg3ServiceApi} built with parameters of this {@code MCg3ServiceApi.Builder}
         */
        public MCg3ServiceApi build() {
            return new MCg3ServiceApi(this);
        }
    }
}
