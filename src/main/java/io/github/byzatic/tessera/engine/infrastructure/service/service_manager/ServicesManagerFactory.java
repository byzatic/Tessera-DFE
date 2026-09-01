package io.github.byzatic.tessera.engine.infrastructure.service.service_manager;

import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.runtime.UnifiedServiceFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Классическая фабрика ServicesManager, совместимая с OrchestrationService.
 * Держит все зависимости ServicesManager и создаёт его, принимая общий scheduler и слушатели событий.
 */
public final class ServicesManagerFactory implements ServicesManagerFactoryInterface {

    private final UnifiedServiceFactory serviceFactory;
    private final StorageManagerInterface storageManager;
    private final FullProjectRepository fullProjectRepository;

    public ServicesManagerFactory(
            @NotNull FullProjectRepository fullProjectRepository,
            @NotNull UnifiedServiceFactory serviceFactory,
            @NotNull StorageManagerInterface storageManager
    ) {
        this.fullProjectRepository = Objects.requireNonNull(fullProjectRepository, "fullProjectRepository");
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "serviceFactory");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
    }

    @Override
    public ServicesManagerInterface create(@NotNull ImmediateSchedulerInterface scheduler,
                                           JobEventListener... listeners) {
        Objects.requireNonNull(scheduler, "scheduler");
        // Используем конструктор ServicesManager с внешним шедуллером и листнерами
        return new ServicesManager(
                fullProjectRepository,
                serviceFactory,
                storageManager,
                scheduler,
                listeners
        );
    }
}
