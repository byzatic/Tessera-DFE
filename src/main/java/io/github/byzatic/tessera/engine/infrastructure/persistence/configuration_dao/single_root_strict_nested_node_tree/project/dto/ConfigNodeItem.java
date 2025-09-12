package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class ConfigNodeItem {

    @SerializedName("name")
    private String name;

    @SerializedName("downstream")
    private List<ConfigNodeItem> downstream;

    @SerializedName("description")
    private String description;

    @SerializedName("id")
    private String id;


    public ConfigNodeItem() {
    }

    private ConfigNodeItem(Builder builder) {
        name = builder.name;
        downstream = builder.downstream;
        description = builder.description;
        id = builder.id;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ConfigNodeItem copy) {
        Builder builder = new Builder();
        builder.name = copy.getName();
        builder.downstream = copy.getDownstream();
        builder.description = copy.getDescription();
        builder.id = copy.getId();
        return builder;
    }


    public String getName() {
        return name;
    }

    public List<ConfigNodeItem> getDownstream() {
        return downstream;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigNodeItem nodeItem = (ConfigNodeItem) o;
        return Objects.equals(name, nodeItem.name) && Objects.equals(downstream, nodeItem.downstream) && Objects.equals(description, nodeItem.description) && Objects.equals(id, nodeItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, downstream, description, id);
    }

    @Override
    public String toString() {
        return "ConfigNodeItem{" +
                "name='" + name + '\'' +
                ", downstream=" + downstream +
                ", description='" + description + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    /**
     * {@code ConfigNodeItem} builder static inner class.
     */
    public static final class Builder {
        private String name;
        private List<ConfigNodeItem> downstream;
        private String description;
        private String id;

        private Builder() {
        }

        /**
         * Sets the {@code name} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code name} to set
         * @return a reference to this Builder
         */
        public Builder name(String val) {
            name = val;
            return this;
        }

        /**
         * Sets the {@code downstream} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code downstream} to set
         * @return a reference to this Builder
         */
        public Builder downstream(List<ConfigNodeItem> val) {
            downstream = val;
            return this;
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
         * Sets the {@code id} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code id} to set
         * @return a reference to this Builder
         */
        public Builder id(String val) {
            id = val;
            return this;
        }

        /**
         * Returns a {@code ConfigNodeItem} built from the parameters previously set.
         *
         * @return a {@code ConfigNodeItem} built with parameters of this {@code ConfigNodeItem.Builder}
         */
        public ConfigNodeItem build() {
            return new ConfigNodeItem(this);
        }
    }
}