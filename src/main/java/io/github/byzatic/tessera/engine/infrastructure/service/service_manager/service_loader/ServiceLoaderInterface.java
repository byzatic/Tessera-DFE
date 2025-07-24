package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import ru.byzatic.metrics_core.service_lib.api_engine.MCg3ServiceApiInterface;
import ru.byzatic.metrics_core.service_lib.service.ServiceInterface;
import ru.byzatic.metrics_core.service_lib.service.health.HealthFlagProxy;

public interface ServiceLoaderInterface {
    ServiceInterface getService(String serviceClassName, MCg3ServiceApiInterface serviceApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException;
}
