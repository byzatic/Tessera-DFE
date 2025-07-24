package io.github.byzatic.tessera.engine.infrastructure.service.service_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.logging.MdcServiceContext;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ServiceItem;
import io.github.byzatic.tessera.engine.domain.model.project.ServicesOptionsItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeProjectGlobalRepositoryInterface;
import io.github.byzatic.tessera.engine.domain.service.ServicesManagerInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto.ServiceDescriptor;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.dto.ServiceParameter;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.MCg3ServiceApi;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.StorageApi;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader.ServiceLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller.JobDetail;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller.SchedulerInterface;
import io.github.byzatic.tessera.engine.domain.repository.storage.StorageManagerInterface;
import ru.byzatic.metrics_core.service_lib.api_engine.MCg3ServiceApiInterface;
import ru.byzatic.metrics_core.service_lib.configuration.ServiceConfigurationParameter;
import ru.byzatic.metrics_core.service_lib.execution_context.ExecutionContextInterface;
import ru.byzatic.metrics_core.service_lib.service.ServiceInterface;
import ru.byzatic.metrics_core.service_lib.service.health.HealthFlagProxy;
import io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_api_interface.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServicesManager implements ServicesManagerInterface {
    private final static Logger logger= LoggerFactory.getLogger(ServicesManager.class);
    private final ServiceLoaderInterface serviceLoader;
    private final StorageManagerInterface storageManager;
    private final SchedulerInterface scheduler;
    private final JpaLikeNodeRepositoryInterface nodeRepository;
    Map<Integer, ServiceDescriptor> serviceDescriptorMap = new ConcurrentHashMap<>();

    public ServicesManager(JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository, ServiceLoaderInterface serviceLoader, StorageManagerInterface storageManager, JpaLikeNodeRepositoryInterface nodeRepository, SchedulerInterface scheduler) {
        this.serviceLoader = serviceLoader;
        this.storageManager = storageManager;
        this.nodeRepository = nodeRepository;
        this.scheduler = scheduler;
        loadDescriptors(projectGlobalRepository);
    }

    private void loadDescriptors(JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository) {
        for (ServiceItem serviceDescription: projectGlobalRepository.getProjectGlobal().getServices()) {
            String serviceId = serviceDescription.getIdName();
            List<ServiceParameter> serviceParameters = new ArrayList<>();
            for (ServicesOptionsItem serviceParameter : serviceDescription.getOptions()) {
                serviceParameters.add(ServiceParameter.newBuilder().setParameterKey(serviceParameter.getName()).setParameterValue(serviceParameter.getData()).build());
            }
            ServiceDescriptor serviceDescriptor = ServiceDescriptor.newBuilder()
                    .setServiceName(serviceId)
                    .setServiceJobId(null)
                    .setServiceParameterList(serviceParameters)
                    .build();
            serviceDescriptorMap.put(
                    serviceDescriptor.hashCode(),
                    serviceDescriptor
            );
            logger.debug("Service with id {} registered in {}", serviceId, this.getClass().getSimpleName());
        }
    }

    @Override
    public void runAllServices() throws OperationIncompleteException {
        logger.debug("Requested run all services");
        try {

            Map<String,ServiceDescriptor> serviceDescriptorChangeMap = new HashMap<>();

            for (Map.Entry<Integer, ServiceDescriptor> serviceDescriptorEntry : serviceDescriptorMap.entrySet()) {
                logger.debug("Processing Map.Entry {}: {}", serviceDescriptorEntry.getClass().getSimpleName(), serviceDescriptorEntry);

                ServiceDescriptor serviceDescriptor = serviceDescriptorEntry.getValue();
                logger.debug("Processing {}: {}", ServiceDescriptor.class.getSimpleName(), serviceDescriptor);

                List<ServiceConfigurationParameter> serviceConfigurationParameters = new ArrayList<>();
                for (ServiceParameter serviceParameter : serviceDescriptor.getServiceParameterList()) {
                    serviceConfigurationParameters.add(ServiceConfigurationParameter.newBuilder()
                            .parameterKey(serviceParameter.getParameterKey())
                            .parameterValue(serviceParameter.getParameterValue())
                            .build());
                }
                logger.debug("Service parameters: {}", serviceConfigurationParameters);

                ExecutionContextInterface executionContext = new ExecutionContext(MdcServiceContext.newBuilder().setServiceName(serviceDescriptor.getServiceName()).build());

                MCg3ServiceApiInterface serviceApi = MCg3ServiceApi.newBuilder()
                        .storageApi(new StorageApi(storageManager, null, nodeRepository))
                        .executionContext(executionContext)
                        .serviceConfigurationParameters(serviceConfigurationParameters)
                        .build();

                ServiceInterface service = serviceLoader.getService(serviceDescriptor.getServiceName(),serviceApi, HealthFlagProxy.newBuilder().build());

                JobDetail jobDetail = JobDetail.newBuilder().job(service).healthFlagProxy(HealthFlagProxy.newBuilder().build()).build();

                scheduler.addJob(jobDetail);

                serviceDescriptorChangeMap.put(jobDetail.getUniqueId(), serviceDescriptor);
            }

            for (Map.Entry<String, ServiceDescriptor> serviceDescriptorEntry : serviceDescriptorChangeMap.entrySet()) {
                updateServiceDescriptorWithProcessId(serviceDescriptorEntry.getValue(), serviceDescriptorEntry.getKey());
            }

            scheduler.runAllJobs(Boolean.FALSE);


        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private void updateServiceDescriptorWithProcessId(ServiceDescriptor serviceDescriptor, String newId) {
        ServiceDescriptor newServiceDescriptor = ServiceDescriptor.newBuilder()
                .setServiceName(serviceDescriptor.getServiceName())
                .setServiceJobId(newId)
                .setServiceParameterList(serviceDescriptor.getServiceParameterList())
                .build();
        serviceDescriptorMap.put(
                serviceDescriptor.hashCode(),
                newServiceDescriptor
        );
    }

    @Override
    public Boolean isAllServicesHealthy() throws OperationIncompleteException {
        boolean result = Boolean.TRUE;
        try {
            for (JobDetail jobDetail : scheduler.getJobDetails()) {
                if (! scheduler.isJobActive(jobDetail)) {
                    result = Boolean.FALSE;
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public void stopAllServices() throws OperationIncompleteException {
        try {
            long defaultForcedTerminationIntervalMinutes = 3L;
            scheduler.removeAllJobs(defaultForcedTerminationIntervalMinutes);
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

}
