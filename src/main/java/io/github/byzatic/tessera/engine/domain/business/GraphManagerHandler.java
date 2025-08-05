package io.github.byzatic.tessera.engine.domain.business;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;

public class GraphManagerHandler implements Runnable{
    private GraphManagerInterface graphManager;

    public GraphManagerHandler() {
    }

    private GraphManagerInterface getGraphManager() {
        return graphManager;
    }

    private GraphManagerHandler(Builder builder) {
        graphManager = builder.graphManager;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GraphManagerHandler copy) {
        Builder builder = new Builder();
        builder.graphManager = copy.getGraphManager();
        return builder;
    }

    @Override
    public void run() {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            graphManager.runGraph();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@code GraphManagerHandler} builder static inner class.
     */
    public static final class Builder {
        private GraphManagerInterface graphManager;

        private Builder() {
        }

        /**
         * Sets the {@code graphManager} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param graphManager the {@code graphManager} to set
         * @return a reference to this Builder
         */
        public Builder setGraphManager(GraphManagerInterface graphManager) {
            this.graphManager = graphManager;
            return this;
        }

        /**
         * Returns a {@code GraphManagerHandler} built from the parameters previously set.
         *
         * @return a {@code GraphManagerHandler} built with parameters of this {@code GraphManagerHandler.Builder}
         */
        public GraphManagerHandler build() {
            return new GraphManagerHandler(this);
        }
    }
}
