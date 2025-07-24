package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto;

import ru.byzatic.commons.ObjectsUtils;

import java.util.Objects;

public class ServiceParameter {
    private String parameterKey = null;
    private String parameterValue = null;

    private ServiceParameter() {
    }

    private ServiceParameter(Builder builder) {
        ObjectsUtils.requireNonNull(builder.parameterKey, new IllegalArgumentException("parameterKey in " + this.getClass().getSimpleName() + " require NonNull"));
        ObjectsUtils.requireNonNull(builder.parameterValue, new IllegalArgumentException("parameterValue in " + this.getClass().getSimpleName() + " require NonNull"));
        parameterKey = builder.parameterKey;
        parameterValue = builder.parameterValue;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ServiceParameter copy) {
        Builder builder = new Builder();
        builder.parameterKey = copy.getParameterKey();
        builder.parameterValue = copy.getParameterValue();
        return builder;
    }

    public String getParameterKey() {
        return parameterKey;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceParameter that = (ServiceParameter) o;
        return Objects.equals(parameterKey, that.parameterKey) && Objects.equals(parameterValue, that.parameterValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterKey, parameterValue);
    }

    /**
     * {@code ServiceParameter} builder static inner class.
     */
    public static final class Builder {
        private String parameterKey;
        private String parameterValue;

        private Builder() {
        }

        /**
         * Sets the {@code parameterKey} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param parameterKey the {@code parameterKey} to set
         * @return a reference to this Builder
         */
        public Builder setParameterKey(String parameterKey) {
            this.parameterKey = parameterKey;
            return this;
        }

        /**
         * Sets the {@code parameterValue} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param parameterValue the {@code parameterValue} to set
         * @return a reference to this Builder
         */
        public Builder setParameterValue(String parameterValue) {
            this.parameterValue = parameterValue;
            return this;
        }

        /**
         * Returns a {@code ServiceParameter} built from the parameters previously set.
         *
         * @return a {@code ServiceParameter} built with parameters of this {@code ServiceParameter.Builder}
         */
        public ServiceParameter build() {
            return new ServiceParameter(this);
        }
    }
}
