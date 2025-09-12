package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.module_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.workflowroutine.api_engine.MCg3WorkflowRoutineApiInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class ModuleLoader implements ModuleLoaderInterface {
    private final static Logger logger = LoggerFactory.getLogger(ModuleLoader.class);
    private final Map<String, WorkflowRoutineFactoryInterface> moduleFactories = new HashMap<>();
    private final Map<String, URLClassLoader> classLoaders = new HashMap<>();

    public ModuleLoader(Path pluginsDirPath, FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        load(pluginsDirPath, fullProjectRepository);
    }

    @Override
    public synchronized WorkflowRoutineInterface getModule(String workflowRoutineClassName, MCg3WorkflowRoutineApiInterface workflowRoutineApi, HealthFlagProxy healthFlagProxy) throws OperationIncompleteException {
        try {
            if (!moduleFactories.containsKey(workflowRoutineClassName))
                throw new OperationIncompleteException("Service with name " + workflowRoutineClassName + " was not found");
            WorkflowRoutineFactoryInterface moduleFactory = moduleFactories.get(workflowRoutineClassName);
            return moduleFactory.create(workflowRoutineApi, healthFlagProxy);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load(Path pluginsDirPath, FullProjectRepository fullProjectRepository) throws OperationIncompleteException {
        @Nullable ClassLoader sharedResources = fullProjectRepository.getSharedResourcesClassLoader();
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

                // classLoader = new URLClassLoader(urls, WorkflowRoutineFactoryInterface.class.getClassLoader());

                if (sharedResources == null) {
                    classLoader = new URLClassLoader(urls, WorkflowRoutineFactoryInterface.class.getClassLoader());
                } else {
                    classLoader = new URLClassLoader(urls, sharedResources);
                }

                java.util.ServiceLoader<WorkflowRoutineFactoryInterface> modules = java.util.ServiceLoader.load(WorkflowRoutineFactoryInterface.class, classLoader);
                logger.debug("modules: {}", modules);

                for (WorkflowRoutineFactoryInterface dummy : modules) {
                    logger.debug("dummy module: {}", dummy);

                    // Class<? extends ServiceFactoryInterface> нельзя потому что стирание типов.
                    // Class<? extends ServiceFactoryInterface> clazz = dummy.getClass();

                    String moduleName = dummy.getClass().getSimpleName().replace("Factory", "");
                    logger.debug("Discovered module: {}", moduleName);

                    if (moduleFactories.containsKey(moduleName))
                        throw new OperationIncompleteException("Found another module with name " + moduleName + " (module duplication)");

                    WorkflowRoutineFactoryInterface moduleFactory = dummy.getClass().getDeclaredConstructor().newInstance();
                    logger.debug("Service Factory created: {}", moduleFactory);

                    moduleFactories.put(moduleName, moduleFactory);
                    logger.debug("Service Factory saved as: {}", moduleName);

                    classLoaders.put(moduleName, classLoader);
                }
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

}
