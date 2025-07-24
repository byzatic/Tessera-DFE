package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import ru.byzatic.metrics_core.mcg3_storageapi_lib.exceptions.MCg3ApiOperationIncompleteException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.GraphPathInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.NodeDescriptionInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.execution_context.StorageDescriptionInterface;

import java.util.List;
import java.util.Objects;

public class NodeDescription implements NodeDescriptionInterface {
    private String name;
    private String id;
    private List<StorageDescriptionInterface> storageDescriptionList;
    private List<GraphPathInterface> rootPaths;

    public NodeDescription() {
    }

    private NodeDescription(Builder builder) {
        name = builder.name;
        id = builder.id;
        storageDescriptionList = builder.storageDescriptionList;
        rootPaths = builder.rootPaths;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(NodeDescription copy) throws OperationIncompleteException {
        try {
            Builder builder = new Builder();
            builder.name = copy.getName();
            builder.id = copy.getId();
            builder.storageDescriptionList = copy.getNodeStorages();
            builder.rootPaths = copy.getRootPaths();
            return builder;
        } catch (MCg3ApiOperationIncompleteException e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<StorageDescriptionInterface> getNodeStorages() {
        return storageDescriptionList;
    }

    @Override
    public List<GraphPathInterface> getRootPaths() throws MCg3ApiOperationIncompleteException {
        return rootPaths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeDescription that = (NodeDescription) o;
        return Objects.equals(name, that.name) && Objects.equals(id, that.id) && Objects.equals(storageDescriptionList, that.storageDescriptionList) && Objects.equals(rootPaths, that.rootPaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, storageDescriptionList, rootPaths);
    }

    @Override
    public String toString() {
        return "NodeDescription{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", storageDescriptionList=" + storageDescriptionList +
                ", rootPaths=" + rootPaths +
                '}';
    }

    /**
     * {@code NodeDescription} builder static inner class.
     */
    public static final class Builder {
        private String name;
        private String id;
        private List<StorageDescriptionInterface> storageDescriptionList;
        private List<GraphPathInterface> rootPaths;

        private Builder() {
        }

        /**
         * Sets the {@code name} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param name the {@code name} to set
         * @return a reference to this Builder
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the {@code id} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param id the {@code id} to set
         * @return a reference to this Builder
         */
        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the {@code storageDescriptionList} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param storageDescriptionList the {@code storageDescriptionList} to set
         * @return a reference to this Builder
         */
        public Builder setStorageDescriptionList(List<StorageDescriptionInterface> storageDescriptionList) {
            this.storageDescriptionList = storageDescriptionList;
            return this;
        }

        /**
         * Sets the {@code rootPaths} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param rootPaths the {@code rootPaths} to set
         * @return a reference to this Builder
         */
        public Builder setRootPaths(List<GraphPathInterface> rootPaths) {
            this.rootPaths = rootPaths;
            return this;
        }

        /**
         * Returns a {@code NodeDescription} built from the parameters previously set.
         *
         * @return a {@code NodeDescription} built with parameters of this {@code NodeDescription.Builder}
         */
        public NodeDescription build() {
            return new NodeDescription(this);
        }
    }
}
