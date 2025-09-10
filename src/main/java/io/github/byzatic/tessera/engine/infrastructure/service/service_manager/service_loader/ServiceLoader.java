package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.service_loader;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.SharedResourcesRepositoryInterface;
import io.github.byzatic.tessera.service.api_engine.MCg3ServiceApiInterface;
import io.github.byzatic.tessera.service.service.ServiceFactoryInterface;
import io.github.byzatic.tessera.service.service.ServiceInterface;
import io.github.byzatic.tessera.service.service.health.HealthFlagProxy;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class ServiceLoader implements ServiceLoaderInterface {
    private final static Logger logger = LoggerFactory.getLogger(ServiceLoader.class);
    private final Map<String, ServiceFactoryInterface> serviceFactories = new HashMap<>();
    private final Map<String, URLClassLoader> classLoaders = new HashMap<>();

    public ServiceLoader(Path pluginsDirPath, SharedResourcesRepositoryInterface sharedResourcesManager) throws OperationIncompleteException {
        load(pluginsDirPath, sharedResourcesManager);
    }

    @Override
    public synchronized ServiceInterface getService(String serviceName, MCg3ServiceApiInterface serviceApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException {
        try {
            if (!serviceFactories.containsKey(serviceName)) throw new OperationIncompleteException("Service with name " + serviceName + " was not found");
            ServiceFactoryInterface serviceFactory = serviceFactories.get(serviceName);
            return serviceFactory.create(serviceApi, healthFlagProxy);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load(Path pluginsDirPath, SharedResourcesRepositoryInterface sharedResourcesManager) throws OperationIncompleteException {
        @Nullable ClassLoader sharedResources = sharedResourcesManager.getSharedResourcesClassLoader();
        File[] jars = null;
        try {
            jars = pluginsDirPath.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars == null) throw new OperationIncompleteException("jar files not found");

            for (File jar : jars) {
                logger.debug("Founded jar: {}", jar);

                URL[] urls = {jar.toURI().toURL()};
                if (logger.isTraceEnabled()) {
                    for (URL url : urls) {
                        logger.trace("Loading from URL: " + url);
                        try (JarFile jarFile = new JarFile(new File(url.toURI()))) {
                            jarFile.stream().forEach(entry -> {
                                if (entry.getName().endsWith(".class")) {
                                    logger.trace("Class found: " + entry.getName());
                                }
                            });
                        }
                    }
                }

                URLClassLoader classLoader;

                // ClassLoader loggingParent = Thread.currentThread().getContextClassLoader();
                // classLoader = new URLClassLoader(urls, loggingParent);

                // classLoader = new URLClassLoader(urls, ServiceFactoryInterface.class.getClassLoader());

                if (sharedResources == null) {
                    classLoader = new URLClassLoader(urls, ServiceFactoryInterface.class.getClassLoader());
                } else {
                    classLoader = new URLClassLoader(urls, sharedResources);
                }

                java.util.ServiceLoader<ServiceFactoryInterface> modules = java.util.ServiceLoader.load(ServiceFactoryInterface.class, classLoader);
                logger.debug("modules: {}", modules);

                for (ServiceFactoryInterface dummy : modules) {
                    logger.debug("dummy module: {}", dummy);

                    // Class<? extends ServiceFactoryInterface> нельзя потому что стирание типов.
                    // Class<? extends ServiceFactoryInterface> clazz = dummy.getClass();

                    String serviceName = dummy.getClass().getSimpleName().replace("Factory", "");
                    logger.debug("Discovered service: {}", serviceName);

                    if (serviceFactories.containsKey(serviceName)) throw new OperationIncompleteException("Found another service with name " + serviceName + " (service duplication)");

                    ServiceFactoryInterface serviceFactory = dummy.getClass().getDeclaredConstructor().newInstance();
                    logger.debug("Service Factory created: {}", serviceFactory);

                    serviceFactories.put(serviceName, serviceFactory);
                    logger.debug("Service Factory saved as: {}", serviceName);

                    classLoaders.put(serviceName, classLoader);
                }
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

}
