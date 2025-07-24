package io.github.byzatic.tessera.engine.application.usecase;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.business.OrchestrationServiceInterface;

public class ProjectOrchestrator {
    private final OrchestrationServiceInterface orchestrationService;

    public ProjectOrchestrator(OrchestrationServiceInterface orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    public void orchestrateProject() throws OperationIncompleteException {
        try {
            orchestrationService.start();
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }
}
