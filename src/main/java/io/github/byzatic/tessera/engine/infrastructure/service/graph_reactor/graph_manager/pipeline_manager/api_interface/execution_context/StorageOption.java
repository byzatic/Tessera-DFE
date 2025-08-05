package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.workflowroutine.execution_context.StorageOptionInterface;

import java.util.Objects;

public class StorageOption implements StorageOptionInterface {
    private String value;
    private String key;

    public StorageOption() {
    }

    private StorageOption(Builder builder) {
        value = builder.value;
        key = builder.key;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(StorageOption copy) {
        Builder builder = new Builder();
        builder.value = copy.getValue();
        builder.key = copy.getKey();
        return builder;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageOption that = (StorageOption) o;
        return Objects.equals(value, that.value) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, key);
    }

    @Override
    public String toString() {
        return "StorageOption{" +
                "value='" + value + '\'' +
                ", key='" + key + '\'' +
                '}';
    }


    /**
     * {@code StorageOption} builder static inner class.
     */
    public static final class Builder {
        private String value;
        private String key;

        private Builder() {
        }

        /**
         * Sets the {@code value} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param value the {@code value} to set
         * @return a reference to this Builder
         */
        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the {@code key} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param key the {@code key} to set
         * @return a reference to this Builder
         */
        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        /**
         * Returns a {@code StorageOption} built from the parameters previously set.
         *
         * @return a {@code StorageOption} built with parameters of this {@code StorageOption.Builder}
         */
        public StorageOption build() {
            return new StorageOption(this);
        }
    }
}
