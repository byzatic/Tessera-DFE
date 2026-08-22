package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.tessera.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.tessera.lib.configio.unified.ServiceCreationRequest;
import io.github.byzatic.tessera.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;

import java.util.Objects;

/**
 * Creates services through the unified project runtime.
 *
 * <p>The owning {@code ProjectRevisionHandle} controls the runtime-session lifecycle.</p>
 */
public final class UnifiedServiceFactory {

    private final ProjectRuntimeSession runtimeSession;

    public UnifiedServiceFactory(ProjectRuntimeSession runtimeSession) {
        this.runtimeSession = Objects.requireNonNull(runtimeSession, "runtimeSession");
    }

    public ServiceInterface create(
            String serviceName,
            MCg3ServiceApiInterface serviceApi,
            HealthFlagProxy health
    ) throws OperationIncompleteException {
        try {
            return runtimeSession.createService(
                    ServiceCreationRequest.newBuilder()
                            .serviceName(serviceName)
                            .api(serviceApi)
                            .health(health)
                            .build()
            );
        } catch (TesseraProjectException exception) {
            throw new OperationIncompleteException(
                    "Cannot create service " + serviceName,
                    exception
            );
        }
    }
}
