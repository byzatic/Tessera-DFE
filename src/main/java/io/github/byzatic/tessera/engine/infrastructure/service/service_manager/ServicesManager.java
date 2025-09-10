package io.github.byzatic.tessera.engine.infrastructure.service.service_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.application.commons.logging.MdcServiceContext;
import io.github.byzatic.tessera.engine.domain.model.project.ServiceItem;
import io.github.byzatic.tessera.engine.domain.model.project.ServicesOptionsItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeProjectGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto.ServiceDescriptor;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto.ServiceParameter;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.ExecutionContext;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.MCg3ServiceApi;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.StorageApi;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoaderInterface;

import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.configuration.ServiceConfigurationParameter;
import io.github.byzatic.tessera.service.execution_context.ExecutionContextInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;

import io.github.byzatic.commons.schedulers.immediate.CancellationToken;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;            // <-- для Builder()
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.JobEventListener;
import io.github.byzatic.commons.schedulers.immediate.JobInfo;
import io.github.byzatic.commons.schedulers.immediate.JobState;
import io.github.byzatic.commons.schedulers.immediate.Task;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * ServicesManager rewritten to use ImmediateScheduler.
 *
 * Semantics:
 * - Loads service descriptors from ProjectGlobal on construction.
 * - runAllServices(): starts any missing services immediately via ImmediateScheduler.
 * - stopAllServices(): requests soft stop (ServiceInterface#terminate) and waits up to a grace timeout
 *   (per-service getTerminationIntervalMinutes() or default 3 minutes), then removes tasks.
 * - Supports external JobEventListener (e.g., business logic) provided via constructor; it is wired
 *   directly into the ImmediateScheduler.
 */
public class ServicesManager implements ServicesManagerInterface {

    private static final Logger logger = LoggerFactory.getLogger(ServicesManager.class);

    private final ServiceLoaderInterface serviceLoader;
    private final StorageManagerInterface storageManager;
    private final JpaLikeNodeRepositoryInterface nodeRepository;
    private final ImmediateSchedulerInterface scheduler;

    /** serviceDescriptorMap keeps original descriptors (keyed by hash). */
    private final Map<Integer, ServiceDescriptor> serviceDescriptorMap = new ConcurrentHashMap<>();

    /** Map serviceName -> jobId for quick existence checks. */
    private final Map<String, UUID> serviceNameToJobId = new ConcurrentHashMap<>();

    /** Map jobId -> service instance so we can terminate with a per-service grace timeout. */
    private final Map<UUID, ServiceInterface> runningServices = new ConcurrentHashMap<>();

    /** Optional external listener (e.g., business logic) */
    private final AtomicReference<JobEventListener> externalListenerRef = new AtomicReference<>(null);

    /** Владеем ли мы внутренним шедуллером (созданным в перегруженном конструкторе) */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean ownsScheduler;

    // ========= Конструктор №1: с внешним шедуллером =========
    public ServicesManager(
            JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository,
            ServiceLoaderInterface serviceLoader,
            StorageManagerInterface storageManager,
            JpaLikeNodeRepositoryInterface nodeRepository,
            ImmediateSchedulerInterface scheduler,
            JobEventListener... listeners // optional: pass from business logic
    ) {
        this.serviceLoader = Objects.requireNonNull(serviceLoader, "serviceLoader");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.nodeRepository = Objects.requireNonNull(nodeRepository, "nodeRepository");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.ownsScheduler = false;

        loadDescriptors(projectGlobalRepository);
        wireListeners(listeners);
    }

    // ========= Конструктор №2: без шедуллера — создаём свой =========
    public ServicesManager(
            JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository,
            ServiceLoaderInterface serviceLoader,
            StorageManagerInterface storageManager,
            JpaLikeNodeRepositoryInterface nodeRepository,
            JobEventListener... listeners // optional
    ) {
        this.serviceLoader = Objects.requireNonNull(serviceLoader, "serviceLoader");
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.nodeRepository = Objects.requireNonNull(nodeRepository, "nodeRepository");
        // Создаём дефолтный ImmediateScheduler через Builder (пул потоков и grace по умолчанию)
        this.scheduler = new ImmediateScheduler.Builder().build();
        this.ownsScheduler = true;

        loadDescriptors(projectGlobalRepository);
        wireListeners(listeners);
    }

    private void wireListeners(JobEventListener... listeners) {
        // Внешние слушатели (если есть) — навешиваем прямо на шедуллер
        if (listeners != null) {
            for (JobEventListener l : listeners) {
                if (l != null) {
                    scheduler.addListener(l);
                    externalListenerRef.set(l); // держим ссылку на последний (опционально)
                }
            }
        }
        // Внутренний логирующий слушатель
        scheduler.addListener(new JobEventListener() {
            @Override public void onStart(UUID jobId) {
                logger.debug("Service job {} started ({})", jobId, serviceNameOf(jobId));
            }
            @Override public void onComplete(UUID jobId) {
                logger.debug("Service job {} completed ({})", jobId, serviceNameOf(jobId));
            }
            @Override public void onError(UUID jobId, Throwable error) {
                logger.warn("Service job {} failed ({}): {}", jobId, serviceNameOf(jobId), error.toString());
            }
            @Override public void onTimeout(UUID jobId) {
                logger.warn("Service job {} timed out ({})", jobId, serviceNameOf(jobId));
            }
            @Override public void onCancelled(UUID jobId) {
                logger.info("Service job {} cancelled ({})", jobId, serviceNameOf(jobId));
            }
        });
    }

    private String serviceNameOf(UUID jobId) {
        for (Map.Entry<String, UUID> e : serviceNameToJobId.entrySet()) {
            if (e.getValue().equals(jobId)) return e.getKey();
        }
        return "?";
    }

    private void loadDescriptors(JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository) {
        Objects.requireNonNull(projectGlobalRepository, "projectGlobalRepository");
        for (ServiceItem serviceDescription : projectGlobalRepository.getProjectGlobal().getServices()) {
            String serviceId = serviceDescription.getIdName();
            List<ServiceParameter> serviceParameters = new ArrayList<>();
            for (ServicesOptionsItem opt : serviceDescription.getOptions()) {
                serviceParameters.add(
                        ServiceParameter.newBuilder()
                                .setParameterKey(opt.getName())
                                .setParameterValue(opt.getData())
                                .build()
                );
            }
            ServiceDescriptor sd = ServiceDescriptor.newBuilder()
                    .setServiceName(serviceId)
                    .setServiceJobId(null)
                    .setServiceParameterList(serviceParameters)
                    .build();
            serviceDescriptorMap.put(sd.hashCode(), sd);
            logger.debug("Service with id {} registered in {}", serviceId, this.getClass().getSimpleName());
        }
    }

    private void updateServiceDescriptorWithProcessId(ServiceDescriptor sd, String newId) {
        ServiceDescriptor newServiceDescriptor = ServiceDescriptor.newBuilder()
                .setServiceName(sd.getServiceName())
                .setServiceJobId(newId)
                .setServiceParameterList(sd.getServiceParameterList())
                .build();
        serviceDescriptorMap.put(sd.hashCode(), newServiceDescriptor);
    }

    @Override
    public void runAllServices() throws OperationIncompleteException {
        logger.debug("Requested run all services");
        try {
            Map<String, ServiceDescriptor> toUpdate = new HashMap<>();

            for (Map.Entry<Integer, ServiceDescriptor> e : serviceDescriptorMap.entrySet()) {
                ServiceDescriptor sd = e.getValue();

                // Если уже запланирован/работает — пропускаем
                UUID existing = serviceNameToJobId.get(sd.getServiceName());
                if (existing != null) {
                    Optional<JobInfo> info = scheduler.query(existing);
                    if (info.isPresent() && (info.get().state == JobState.SCHEDULED || info.get().state == JobState.RUNNING)) {
                        logger.debug("Service {} already scheduled/running as {}", sd.getServiceName(), existing);
                        continue;
                    }
                    serviceNameToJobId.remove(sd.getServiceName());
                }

                // MDC/ExecutionContext
                ExecutionContextInterface executionContext =
                        new ExecutionContext(MdcServiceContext.newBuilder()
                                .setServiceName(sd.getServiceName())
                                .build());

                // Параметры сервиса
                List<ServiceConfigurationParameter> params = sd.getServiceParameterList().stream()
                        .map(p -> ServiceConfigurationParameter.newBuilder()
                                .parameterKey(p.getParameterKey())
                                .parameterValue(p.getParameterValue())
                                .build())
                        .collect(Collectors.toList());
                logger.debug("Service parameters for {}: {}", sd.getServiceName(), params);

                // API + загрузка сервиса
                MCg3ServiceApiInterface serviceApi = MCg3ServiceApi.newBuilder()
                        .storageApi(new StorageApi(storageManager, null, nodeRepository))
                        .executionContext(executionContext)
                        .serviceConfigurationParameters(params)
                        .build();

                ServiceInterface service = serviceLoader.getService(
                        sd.getServiceName(),
                        serviceApi,
                        HealthFlagProxy.newBuilder().build()
                );

                // Обёртка в Task
                Task task = new ServiceTask(service);

                UUID jobId = scheduler.addTask(task);

                runningServices.put(jobId, service);
                serviceNameToJobId.put(sd.getServiceName(), jobId);
                toUpdate.put(jobId.toString(), sd);

                logger.info("Service {} scheduled as {}", sd.getServiceName(), jobId);
            }

            // Проставляем jobId в дескрипторы
            for (Map.Entry<String, ServiceDescriptor> p : toUpdate.entrySet()) {
                updateServiceDescriptorWithProcessId(p.getValue(), p.getKey());
            }

            // ImmediateScheduler сам стартует таски при addTask()

        } catch (Exception ex) {
            throw new OperationIncompleteException(ex);
        }
    }

    @Override
    public void stopAllServices() throws OperationIncompleteException {
        try {
            for (Map.Entry<String, UUID> e : new ArrayList<>(serviceNameToJobId.entrySet())) {
                UUID jobId = e.getValue();
                ServiceInterface service = runningServices.get(jobId);

                long minutes = 3L;
                if (service != null) {
                    Long m = service.getTerminationIntervalMinutes();
                    if (m != null && m > 0) minutes = m;
                }

                Duration grace = Duration.ofMinutes(minutes);
                scheduler.removeTask(jobId, grace);

                runningServices.remove(jobId);
                serviceNameToJobId.remove(e.getKey());
                logger.info("Service {} stop requested (grace {} min), job {}", e.getKey(), minutes, jobId);
            }
        } catch (Exception ex) {
            throw new OperationIncompleteException(ex);
        }
    }

    /**
     * Simple Task wrapper around ServiceInterface.
     * - run(): delegates to service.run()
     * - onStopRequested(): delegates to service.terminate()
     */
    private static final class ServiceTask implements Task {
        private final ServiceInterface service;

        private ServiceTask(ServiceInterface service) {
            this.service = Objects.requireNonNull(service, "service");
        }

        @Override
        public void run(CancellationToken token) throws Exception {
            try {
                Thread.currentThread().setName("service-" + safeName());
            } catch (Throwable ignore) { }
            service.run();
        }

        @Override
        public void onStopRequested() {
            try {
                service.terminate();
            } catch (Exception ex) {
                LoggerFactory.getLogger(ServiceTask.class)
                        .warn("Service {} terminate() threw: {}", safeName(), ex.toString());
            } catch (Throwable t) {
                LoggerFactory.getLogger(ServiceTask.class)
                        .warn("Service {} terminate() unexpected error: {}", safeName(), t.toString());
            }
        }

        private String safeName() {
            try {
                return String.valueOf(service.getName());
            } catch (Throwable t) {
                return "unknown";
            }
        }
    }
}