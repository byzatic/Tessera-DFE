package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;

public interface ServiceLoaderInterface {
    ServiceInterface getService(String serviceClassName, MCg3ServiceApiInterface serviceApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException;
}
