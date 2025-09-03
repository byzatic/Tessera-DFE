package io.github.byzatic.tessera.engine.domain.service;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import org.jetbrains.annotations.NotNull;

public interface GraphManagerFactoryInterface {
    GraphManagerInterface create(JobEventListener... listeners);

    GraphManagerInterface create(@NotNull ImmediateSchedulerInterface scheduler,
                                 JobEventListener... listeners);
}
