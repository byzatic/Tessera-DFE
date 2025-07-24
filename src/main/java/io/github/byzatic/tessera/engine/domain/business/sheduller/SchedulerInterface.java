package io.github.byzatic.tessera.engine.domain.business.sheduller;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface SchedulerInterface {
    void addTask(JobDetail jobDetail);

    void runAllTasks(Boolean isJoinThreads);

    void checkHealth() throws OperationIncompleteException;
}
