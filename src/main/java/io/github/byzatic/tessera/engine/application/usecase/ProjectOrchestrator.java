package io.github.byzatic.tessera.engine.application.usecase;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectOrchestrator {
    private final OrchestrationServiceInterface orchestrationService;
    private static final Logger logger = LoggerFactory.getLogger(ProjectOrchestrator.class);

    public ProjectOrchestrator(OrchestrationServiceInterface orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    public void orchestrateProject() throws OperationIncompleteException {
        try (orchestrationService) {
            logger.debug("Starts Project Orchestrator");
            orchestrationService.start();
            logger.debug("Down Project Orchestrator");
        } catch (Exception e) {
            logger.error("Error Project Orchestrator: {}", e.getMessage());
            throw new OperationIncompleteException(e);
        }
    }
}
