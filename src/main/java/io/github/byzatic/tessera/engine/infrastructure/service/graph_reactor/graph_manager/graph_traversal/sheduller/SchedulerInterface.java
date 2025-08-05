package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface SchedulerInterface {
    void addJob(JobDetail jobDetail) throws OperationIncompleteException;

    void runAllJobs() throws OperationIncompleteException;

    void cleanup() throws OperationIncompleteException;

    Boolean isJobActive(JobDetail jobDetail) throws OperationIncompleteException;
}
