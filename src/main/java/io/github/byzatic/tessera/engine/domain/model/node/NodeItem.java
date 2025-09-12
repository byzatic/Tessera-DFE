package io.github.byzatic.tessera.engine.domain.model.node;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;

import java.util.List;
import java.util.Objects;

public class NodeItem {
    private String uuid;
    private String id;
    private String name;
    private String description;
    private List<GraphNodeRef> downstream;

    public NodeItem() {
    }

    private NodeItem(Builder builder) {
        uuid = builder.uuid;
        id = builder.id;
        name = builder.name;
        description = builder.description;
        downstream = builder.downstream;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(NodeItem copy) {
        Builder builder = new Builder();
        builder.uuid = copy.getUUID();
        builder.id = copy.getId();
        builder.name = copy.getName();
        builder.description = copy.getDescription();
        builder.downstream = copy.getDownstream();
        return builder;
    }

    public String getUUID() {
        return uuid;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<GraphNodeRef> getDownstream() {
        return downstream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeItem nodeItem = (NodeItem) o;
        return Objects.equals(uuid, nodeItem.uuid) && Objects.equals(id, nodeItem.id) && Objects.equals(name, nodeItem.name) && Objects.equals(description, nodeItem.description) && Objects.equals(downstream, nodeItem.downstream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, id, name, description, downstream);
    }

    @Override
    public String toString() {
        return "NodeItem{" +
                "uuid='" + uuid + '\'' +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", downstream=" + downstream +
                '}';
    }

    /**
     * {@code NodeItem} builder static inner class.
     */
    public static final class Builder {
        private String uuid;
        private String id;
        private String name;
        private String description;
        private List<GraphNodeRef> downstream;

        private Builder() {
        }

        /**
         * Sets the {@code uuid} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param uuid the {@code uuid} to set
         * @return a reference to this Builder
         */
        public Builder setUUID(String uuid) {
            this.uuid = uuid;
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
         * Sets the {@code downstream} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param downstream the {@code downstream} to set
         * @return a reference to this Builder
         */
        public Builder setDownstream(List<GraphNodeRef> downstream) {
            this.downstream = downstream;
            return this;
        }

        /**
         * Returns a {@code NodeItem} built from the parameters previously set.
         *
         * @return a {@code NodeItem} built with parameters of this {@code NodeItem.Builder}
         */
        public NodeItem build() {
            return new NodeItem(this);
        }
    }
}