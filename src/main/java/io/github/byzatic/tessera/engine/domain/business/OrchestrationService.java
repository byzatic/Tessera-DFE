package io.github.byzatic.tessera.engine.domain.business;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.BusinessLogicException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.business.sheduller.JobDetail;
import io.github.byzatic.tessera.engine.domain.business.sheduller.SchedulerInterface;
import io.github.byzatic.tessera.engine.domain.service.GraphManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;

import java.text.ParseException;

public class OrchestrationService implements OrchestrationServiceInterface {
    private final static Logger logger= LoggerFactory.getLogger(OrchestrationService.class);
    private final SchedulerInterface scheduler;
    private final ServicesManagerInterface serviceManager;
    private Integer state = 0;

    public OrchestrationService(@NotNull SchedulerInterface scheduler, @NotNull ServicesManagerInterface serviceManager, @NotNull GraphManagerInterface graphManager) {
        this.scheduler = scheduler;
        this.serviceManager = serviceManager;
        try {
            scheduler.addTask(
                    JobDetail.newBuilder()
                            .setJob(GraphManagerHandler.newBuilder().setGraphManager(graphManager).build())
                            .setCronExpressionString(Configuration.CRON_EXPRESSION_STRING)
                            .build()
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() throws BusinessLogicException {
        try {

            serviceManager.runAllServices();
            scheduler.runAllTasks(false);

            do {

                scheduler.runAllTasks(false);

                scheduler.checkHealth();

                if ( ! serviceManager.isAllServicesHealthy() ) {
                    throw new OperationIncompleteException("Services runtime error");
                }

                systemDelay();

            } while (state == 0);
        } catch (Exception e) {
            logger.error("Business logic failed cause: {}", e.getMessage());
            logger.error("Business logic fail cause trace", e);
            terminate();
            throw new BusinessLogicException(e.getMessage(), e);
        }
    }

    private void terminate() throws BusinessLogicException {
        try {
            serviceManager.stopAllServices();
            // TODO: terminate graph calculation
            state = 1;
        } catch (Exception e) {
            throw new BusinessLogicException(e.getMessage(), e);
        }
    }

    private void systemDelay() throws InterruptedException {
        // TODO: Inspection 'Busy wait' has no quick-fixes for this problem.
        //       Click to edit inspection options, suppress the warning, or disable the inspection completely.
        Thread.sleep(10L);
    }
}
