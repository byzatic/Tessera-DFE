package io.github.byzatic.tessera.engine.domain.model.node_pipeline;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class ConfigurationFilesItem {

    @SerializedName("description")
    private String description;

    @SerializedName("configuration_file_id")
    private String configurationFileId;

    public ConfigurationFilesItem() {
    }

    private ConfigurationFilesItem(Builder builder) {
        description = builder.description;
        configurationFileId = builder.configurationFileId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ConfigurationFilesItem copy) {
        Builder builder = new Builder();
        builder.description = copy.getDescription();
        builder.configurationFileId = copy.getConfigurationFileId();
        return builder;
    }

    public String getDescription() {
        return description;
    }

    public String getConfigurationFileId() {
        return configurationFileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationFilesItem that = (ConfigurationFilesItem) o;
        return Objects.equals(description, that.description) && Objects.equals(configurationFileId, that.configurationFileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, configurationFileId);
    }

    @Override
    public String toString() {
        return "ConfigurationFilesItem{" +
                "description='" + description + '\'' +
                ", configurationFileId='" + configurationFileId + '\'' +
                '}';
    }

    /**
     * {@code ConfigurationFilesItem} builder static inner class.
     */
    public static final class Builder {
        private String description;
        private String configurationFileId;

        private Builder() {
        }

        /**
         * Sets the {@code description} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code description} to set
         * @return a reference to this Builder
         */
        public Builder description(String val) {
            description = val;
            return this;
        }

        /**
         * Sets the {@code configurationFileId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code configurationFileId} to set
         * @return a reference to this Builder
         */
        public Builder configurationFileId(String val) {
            configurationFileId = val;
            return this;
        }

        /**
         * Returns a {@code ConfigurationFilesItem} built from the parameters previously set.
         *
         * @return a {@code ConfigurationFilesItem} built with parameters of this {@code ConfigurationFilesItem.Builder}
         */
        public ConfigurationFilesItem build() {
            return new ConfigurationFilesItem(this);
        }
    }
}