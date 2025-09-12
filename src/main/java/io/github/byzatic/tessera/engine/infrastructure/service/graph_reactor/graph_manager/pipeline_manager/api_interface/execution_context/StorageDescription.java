package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.engine.domain.model.node_global.OptionsItem;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesOptionsItem;
import io.github.byzatic.tessera.workflowroutine.execution_context.StorageDescriptionInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.StorageOptionInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StorageDescription implements StorageDescriptionInterface {
    private String idName;
    private String description;
    private List<StorageOptionInterface> options;

    public StorageDescription() {
    }

    private StorageDescription(Builder builder) {
        idName = builder.idName;
        description = builder.description;
        options = builder.options;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(StorageDescription copy) {
        Builder builder = new Builder();
        builder.idName = copy.getIdName();
        builder.description = copy.getDescription();
        builder.options = copy.getOptions();
        return builder;
    }

    public static Builder newBuilder(StoragesItem globalStorageItem) {
        Builder builder = new Builder();
        builder.idName = String.copyValueOf(globalStorageItem.getIdName().toCharArray());
        builder.description = String.copyValueOf(globalStorageItem.getDescription().toCharArray());
        builder.options = convertGlobalStoragesItemOptions(globalStorageItem);
        return builder;
    }

    public static Builder newBuilder(io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem nodeStorageItem) {
        Builder builder = new Builder();
        builder.idName = String.copyValueOf(nodeStorageItem.getIdName().toCharArray());
        builder.description = String.copyValueOf(nodeStorageItem.getDescription().toCharArray());
        builder.options = convertNodeStoragesItemOptions(nodeStorageItem);
        return builder;
    }

    private static List<StorageOptionInterface> convertGlobalStoragesItemOptions(StoragesItem storagesItem) {
        List<StorageOptionInterface> options = new ArrayList<>();
        for (StoragesOptionsItem option : storagesItem.getOptions()) {
            options.add(StorageOption.newBuilder()
                    .setKey(String.copyValueOf(option.getKey().toCharArray()))
                    .setValue(String.copyValueOf(option.getValue().toCharArray()))
                    .build());
        }
        return options;

    }

    private static List<StorageOptionInterface> convertNodeStoragesItemOptions(io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem storagesItem) {
        List<StorageOptionInterface> options = new ArrayList<>();
        for (OptionsItem option : storagesItem.getOptions()) {
            options.add(StorageOption.newBuilder()
                    .setKey(String.copyValueOf(option.getKey().toCharArray()))
                    .setValue(String.copyValueOf(option.getValue().toCharArray()))
                    .build());
        }
        return options;

    }

    @Override
    public String getIdName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public List<StorageOptionInterface> getOptions() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageDescription that = (StorageDescription) o;
        return Objects.equals(idName, that.idName) && Objects.equals(description, that.description) && Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idName, description, options);
    }

    @Override
    public String toString() {
        return "StorageDescription{" +
                "idName='" + idName + '\'' +
                ", description='" + description + '\'' +
                ", options=" + options +
                '}';
    }

    /**
     * {@code StorageDescription} builder static inner class.
     */
    public static final class Builder {
        private String idName;
        private String description;
        private List<StorageOptionInterface> options;

        private Builder() {
        }

        /**
         * Sets the {@code idName} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param idName the {@code idName} to set
         * @return a reference to this Builder
         */
        public Builder setIdName(String idName) {
            this.idName = idName;
            return this;
        }

        /**
         * Sets the {@code description} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param description the {@code description} to set
         * @return a reference to this Builder
         */
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the {@code options} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param options the {@code options} to set
         * @return a reference to this Builder
         */
        public Builder setOptions(List<StorageOptionInterface> options) {
            this.options = options;
            return this;
        }

        /**
         * Returns a {@code StorageDescription} built from the parameters previously set.
         *
         * @return a {@code StorageDescription} built with parameters of this {@code StorageDescription.Builder}
         */
        public StorageDescription build() {
            return new StorageDescription(this);
        }
    }
}
