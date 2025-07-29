package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto;

import io.github.byzatic.commons.ObjectsUtils;

import java.util.List;
import java.util.Objects;

public class ServiceDescriptor {
    private String serviceName = null;
    private String serviceJobId = null;
    private List<ServiceParameter> serviceParameterList = null;

    private ServiceDescriptor() {
    }

    private ServiceDescriptor(Builder builder) {
        ObjectsUtils.requireNonNull(builder.serviceName, new IllegalArgumentException("serviceName in " + this.getClass().getSimpleName() + " require NonNull"));
        ObjectsUtils.requireNonNull(builder.serviceParameterList, new IllegalArgumentException("serviceParameterList in " + this.getClass().getSimpleName() + " require NonNull"));
        serviceName = builder.serviceName;
        serviceJobId = builder.serviceJobId;
        serviceParameterList = builder.serviceParameterList;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ServiceDescriptor copy) {
        Builder builder = new Builder();
        builder.serviceName = copy.getServiceName();
        builder.serviceJobId = copy.getServiceJobId();
        builder.serviceParameterList = copy.getServiceParameterList();
        return builder;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceJobId() {
        return serviceJobId;
    }

    public List<ServiceParameter> getServiceParameterList() {
        return serviceParameterList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDescriptor that = (ServiceDescriptor) o;
        return Objects.equals(serviceName, that.serviceName) && Objects.equals(serviceJobId, that.serviceJobId) && Objects.equals(serviceParameterList, that.serviceParameterList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceJobId, serviceParameterList);
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceJobId='" + serviceJobId + '\'' +
                ", serviceParameterList=" + serviceParameterList +
                '}';
    }

    /**
     * {@code ServiceDescriptor} builder static inner class.
     */
    public static final class Builder {
        private String serviceName;
        private String serviceJobId;
        private List<ServiceParameter> serviceParameterList;

        private Builder() {
        }

        /**
         * Sets the {@code serviceName} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param serviceName the {@code serviceName} to set
         * @return a reference to this Builder
         */
        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        /**
         * Sets the {@code serviceJobId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param serviceJobId the {@code serviceJobId} to set
         * @return a reference to this Builder
         */
        public Builder setServiceJobId(String serviceJobId) {
            this.serviceJobId = serviceJobId;
            return this;
        }

        /**
         * Sets the {@code serviceParameterList} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param serviceParameterList the {@code serviceParameterList} to set
         * @return a reference to this Builder
         */
        public Builder setServiceParameterList(List<ServiceParameter> serviceParameterList) {
            this.serviceParameterList = serviceParameterList;
            return this;
        }

        /**
         * Returns a {@code ServiceDescriptor} built from the parameters previously set.
         *
         * @return a {@code ServiceDescriptor} built with parameters of this {@code ServiceDescriptor.Builder}
         */
        public ServiceDescriptor build() {
            return new ServiceDescriptor(this);
        }
    }
}
