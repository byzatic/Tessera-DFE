package io.github.byzatic.tessera.engine.domain.model;

import java.util.Objects;

/**
 * Если сущность представляет ключ для извлечения большого объекта, важно, чтобы имя передавало её предназначение: служить маркером, ссылкой или идентификатором. Например:
 * 1.	DataReference – если объект больше напоминает ссылку на данные.
 * 2.	NodeReference – если речь идет о конкретной ноде графа.
 * 3.	NodeKey – если акцент на том, что это ключ для получения данных ноды.
 * 4.	GraphNodeRef – для конкретизации связи с графами.
 * 5.	EntityIdentifier – если система работает с более обширной доменной моделью.
 * Таким образом, подход зависит от контекста доменной области и читаемости для разработчиков, работающих с вашим кодом.
 */
public class GraphNodeRef {
    private String nodeUUID;

    public GraphNodeRef() {
    }

    private GraphNodeRef(Builder builder) {
        nodeUUID = builder.nodeUUID;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GraphNodeRef copy) {
        Builder builder = new Builder();
        builder.nodeUUID = copy.getNodeUUID();
        return builder;
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeRef that = (GraphNodeRef) o;
        return Objects.equals(nodeUUID, that.nodeUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeUUID);
    }

    @Override
    public String toString() {
        return "GraphNodeRef{" +
                "nodeID='" + nodeUUID + '\'' +
                '}';
    }

    /**
     * {@code GraphNodeRef} builder static inner class.
     */
    public static final class Builder {
        private String nodeUUID;

        private Builder() {
        }

        /**
         * Sets the {@code nodeID} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code nodeID} to set
         * @return a reference to this Builder
         */
        public Builder nodeUUID(String val) {
            nodeUUID = val;
            return this;
        }

        /**
         * Returns a {@code GraphNodeRef} built from the parameters previously set.
         *
         * @return a {@code GraphNodeRef} built with parameters of this {@code GraphNodeRef.Builder}
         */
        public GraphNodeRef build() {
            return new GraphNodeRef(this);
        }
    }
}
