package io.github.byzatic.tessera.engine.domain.service;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;

/**
 * Фабрика для создания ServicesManager с передачей scheduler и listeners.
 */
// TODO: java: Unexpected @FunctionalInterface annotation
//@FunctionalInterface
public interface ServicesManagerFactoryInterface {
    ServicesManagerInterface create(JobEventListener... listeners);

    ServicesManagerInterface create(ImmediateSchedulerInterface scheduler, JobEventListener... listeners);
}
