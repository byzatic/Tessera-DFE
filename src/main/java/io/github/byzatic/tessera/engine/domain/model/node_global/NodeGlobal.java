package io.github.byzatic.tessera.engine.domain.model.node_global;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class NodeGlobal {

    @SerializedName("storages")
    private List<StoragesItem> storages;

    public NodeGlobal() {
    }

    private NodeGlobal(Builder builder) {
        storages = builder.storages;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(NodeGlobal copy) {
        Builder builder = new Builder();
        builder.storages = copy.getStorages();
        return builder;
    }

    public List<StoragesItem> getStorages() {
        return storages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeGlobal that = (NodeGlobal) o;
        return Objects.equals(storages, that.storages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storages);
    }

    @Override
    public String toString() {
        return "NodeGlobal{" +
                "storages=" + storages +
                '}';
    }

    /**
     * {@code NodeGlobal} builder static inner class.
     */
    public static final class Builder {
        private List<StoragesItem> storages;

        private Builder() {
        }

        /**
         * Sets the {@code storages} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code storages} to set
         * @return a reference to this Builder
         */
        public Builder storages(List<StoragesItem> val) {
            storages = val;
            return this;
        }

        /**
         * Returns a {@code NodeGlobal} built from the parameters previously set.
         *
         * @return a {@code NodeGlobal} built with parameters of this {@code NodeGlobal.Builder}
         */
        public NodeGlobal build() {
            return new NodeGlobal(this);
        }
    }
}