package io.github.byzatic.tessera.engine.infrastructure.service.service_manager;

import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeProjectGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerFactoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoaderInterface;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Классическая фабрика ServicesManager, совместимая с OrchestrationService.
 * Держит все зависимости ServicesManager и создаёт его, принимая общий scheduler и слушатели событий.
 */
public final class ServicesManagerFactory implements ServicesManagerFactoryInterface {

    private final JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository;
    private final ServiceLoaderInterface serviceLoader;
    private final StorageManagerInterface storageManager;
    private final JpaLikeNodeRepositoryInterface nodeRepository;

    public ServicesManagerFactory(
            @NotNull JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository,
            @NotNull ServiceLoaderInterface serviceLoader,
            @NotNull StorageManagerInterface storageManager,
            @NotNull JpaLikeNodeRepositoryInterface nodeRepository
    ) {
        this.projectGlobalRepository = Objects.requireNonNull(projectGlobalRepository, "projectGlobalRepository");
        this.serviceLoader = Objects.requireNonNull(serviceLoader, "serviceLoader");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.nodeRepository = Objects.requireNonNull(nodeRepository, "nodeRepository");
    }

    @Override
    public ServicesManagerInterface create(JobEventListener... listeners) {
        // Используем конструктор ServicesManager с листнерами
        return new ServicesManager(
                projectGlobalRepository,
                serviceLoader,
                storageManager,
                nodeRepository,
                listeners
        );
    }

    @Override
    public ServicesManagerInterface create(@NotNull ImmediateSchedulerInterface scheduler,
                                           JobEventListener... listeners) {
        Objects.requireNonNull(scheduler, "scheduler");
        // Используем конструктор ServicesManager с внешним шедуллером и листнерами
        return new ServicesManager(
                projectGlobalRepository,
                serviceLoader,
                storageManager,
                nodeRepository,
                scheduler,
                listeners
        );
    }
}