package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.lib.configio.domain.exception.PluginLoadingException;
import io.github.byzatic.lib.configio.unified.ProjectRuntimeSession;
import io.github.byzatic.lib.configio.unified.ServiceCreationRequest;
import io.github.byzatic.lib.configio.unified.TesseraProjectException;
import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;

import java.util.Objects;
import java.util.Set;

/**
 * Adapts the unified runtime session to the service loader used by the engine.
 */
final class RuntimeServiceLoaderAdapter implements ServiceLoaderInterface {

    private final ProjectRuntimeSession runtimeSession;

    RuntimeServiceLoaderAdapter(ProjectRuntimeSession runtimeSession) {
        this.runtimeSession = Objects.requireNonNull(runtimeSession, "runtimeSession");
    }

    @Override
    public ServiceInterface getService(
            String serviceClassName,
            MCg3ServiceApiInterface serviceApi,
            HealthFlagProxy healthFlagProxy
    ) throws PluginLoadingException {
        try {
            return runtimeSession.createService(
                    ServiceCreationRequest.newBuilder()
                            .serviceName(serviceClassName)
                            .api(serviceApi)
                            .health(healthFlagProxy)
                            .build()
            );
        } catch (TesseraProjectException exception) {
            throw new PluginLoadingException(
                    "Cannot create service " + serviceClassName,
                    exception
            );
        }
    }

    @Override
    public Set<String> getAvailableServiceNames() {
        return runtimeSession.getAvailableServiceNames();
    }

    @Override
    public void close() {
        // The project revision handle owns and closes the runtime session.
    }
}
