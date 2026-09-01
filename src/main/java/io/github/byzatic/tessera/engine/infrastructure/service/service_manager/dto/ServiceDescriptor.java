package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto;

import java.util.List;
import java.util.Objects;

public class ServiceDescriptor {
    private String serviceName = null;
    private List<ServiceParameter> serviceParameterList = null;

    private ServiceDescriptor() {
    }

    private ServiceDescriptor(Builder builder) {
        if (builder.serviceName == null) {
            throw new IllegalArgumentException("serviceName in " + getClass().getSimpleName() + " require NonNull");
        }
        if (builder.serviceParameterList == null) {
            throw new IllegalArgumentException("serviceParameterList in " + getClass().getSimpleName() + " require NonNull");
        }
        serviceName = builder.serviceName;
        serviceParameterList = List.copyOf(builder.serviceParameterList);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ServiceDescriptor copy) {
        Builder builder = new Builder();
        builder.serviceName = copy.getServiceName();
        builder.serviceParameterList = copy.getServiceParameterList();
        return builder;
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<ServiceParameter> getServiceParameterList() {
        return serviceParameterList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDescriptor that = (ServiceDescriptor) o;
        return Objects.equals(serviceName, that.serviceName) && Objects.equals(serviceParameterList, that.serviceParameterList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceParameterList);
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceParameterList=" + serviceParameterList +
                '}';
    }

    /**
     * {@code ServiceDescriptor} builder static inner class.
     */
    public static final class Builder {
        private String serviceName;
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
