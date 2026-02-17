package io.github.byzatic.tessera.engine.application.usecase;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import io.github.byzatic.tessera.engine.infrastructure.observability.PrometheusMetricsAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectOrchestrator {
    private final OrchestrationServiceInterface orchestrationService;
    private static final Logger logger = LoggerFactory.getLogger(ProjectOrchestrator.class);
    private PrometheusMetricsAgent prometheusMetricsAgent = null;

    public ProjectOrchestrator(OrchestrationServiceInterface orchestrationService) throws OperationIncompleteException {
        try {
            this.orchestrationService = orchestrationService;

            // Enable Prometheus metrics agent
            this.prometheusMetricsAgent = PrometheusMetricsAgent.getInstance();
            this.prometheusMetricsAgent.start(Configuration.PROMETHEUS_URI);
            if (Configuration.JVM_METRICS_ENABLED) this.prometheusMetricsAgent.enableJvmMetrics();

        } catch (Exception e) {
            logger.error("Error Project Orchestrator creation: {}", e.getMessage());
            throw new OperationIncompleteException(e);
        }
    }

    public void orchestrateProject() throws OperationIncompleteException {
        try (orchestrationService) {
            logger.debug("Starts Project Orchestrator");
            orchestrationService.start();
            logger.debug("Down Project Orchestrator");
        } catch (Exception e) {
            logger.error("Error Project Orchestrator: {}", e.getMessage());
            this.prometheusMetricsAgent.stop();
            throw new OperationIncompleteException(e);
        }
    }
}
