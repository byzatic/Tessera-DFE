package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

import java.util.List;

public interface SchedulerInterface {
    void addJob(JobDetail jobDetail) throws OperationIncompleteException;

    void removeAllJobs(Long defaultForcedTerminationIntervalMinutes) throws OperationIncompleteException;

    void removeJob(JobDetail jobDetail, Long defaultForcedTerminationIntervalMinutes) throws OperationIncompleteException;

    void runAllJobs(Boolean isJoinThreads) throws OperationIncompleteException;

    Boolean isJobActive(JobDetail jobDetail) throws OperationIncompleteException;

    List<JobDetail> getJobDetails();
}
