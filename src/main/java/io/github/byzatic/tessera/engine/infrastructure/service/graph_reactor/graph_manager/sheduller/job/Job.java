package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.job;

import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.health.HealthFlag;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.health.HealthStateProxy;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.dto.Node;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.GraphTraversalInterface;

public class Job implements JobInterface {
    private final static Logger logger= LoggerFactory.getLogger(Job.class);
    private final GraphTraversalInterface graphTraversal;
    private final Node rootNode;
    private final HealthStateProxy healthStateProxy;

    public Job(@NotNull GraphTraversalInterface graphTraversal, @NotNull Node rootNode, HealthStateProxy healthStateProxy) {
        ObjectsUtils.requireNonNull(graphTraversal, new IllegalArgumentException(GraphTraversalInterface.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(rootNode, new IllegalArgumentException(Node.class.getSimpleName() + " should be NotNull"));
        ObjectsUtils.requireNonNull(healthStateProxy, new IllegalArgumentException(HealthStateProxy.class.getSimpleName() + " should be NotNull"));
        this.graphTraversal = graphTraversal;
        this.rootNode = rootNode;
        this.healthStateProxy = healthStateProxy;
    }

    @Override
    public void run() {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            healthStateProxy.setHealthFlag(HealthFlag.RUNNING);
            logger.debug("Node processing -> RUNNING");
            this.graphTraversal.traverse(this.rootNode);
            healthStateProxy.setHealthFlag(HealthFlag.FINISHED);
            logger.debug("Node processing -> FINISHED");
        } catch (Exception e) {
            healthStateProxy.setThrowable(e);
            healthStateProxy.setHealthFlag(HealthFlag.ERROR);
            logger.debug("Node processing -> ERROR");
            throw new RuntimeException(e);
        }
    }
}
