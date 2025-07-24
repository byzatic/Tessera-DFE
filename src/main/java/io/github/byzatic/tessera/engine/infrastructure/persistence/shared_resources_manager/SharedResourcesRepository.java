package io.github.byzatic.tessera.engine.infrastructure.persistence.shared_resources_manager;

import com.google.errorprone.annotations.ThreadSafe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.repository.SharedResourcesRepositoryInterface;
import ru.byzatic.metrics_core.service_lib.service.ServiceFactoryInterface;
import ru.byzatic.metrics_core.workflowroutines_lib.workflowroutines.WorkflowRoutineFactoryInterface;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@ThreadSafe
public class SharedResourcesRepository implements SharedResourcesRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(SharedResourcesRepository.class);
    private final List<ClassLoader> urlClassLoaders = new ArrayList<>();

    public SharedResourcesRepository(String projectName) throws OperationIncompleteException {
        try {
            loadSharedResources(projectName);
        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    @Override
    public synchronized @Nullable ClassLoader getSharedResourcesClassLoader() {
        ClassLoader lastUrlClassLoader = null;
        if (!urlClassLoaders.isEmpty()) {
            lastUrlClassLoader = urlClassLoaders.get(urlClassLoaders.size() - 1);
        }
        return lastUrlClassLoader;
    }

    private void loadSharedResources(String projectName) throws OperationIncompleteException {
        try {
            initPreloadedResources();

            File dir = Configuration.PROJECTS_DIR.resolve(projectName).resolve("modules").resolve("shared").toFile();

            if (!dir.exists())
                throw new OperationIncompleteException("Shared resources directory not exists! (" + dir + ")");

            File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    logger.debug("Found jar: " + jar.getAbsolutePath());
                    loadResource(jar);
                    logger.debug("Loaded jar: " + jar.getAbsolutePath());
                }
            } else {
                logger.debug("No jars found or directory not valid");
            }

        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private void loadResource(File file) throws OperationIncompleteException {
        try {
            ClassLoader lastClassLoader = null;

            if (!urlClassLoaders.isEmpty()) {
                lastClassLoader = urlClassLoaders.get(urlClassLoaders.size() - 1);
            }

            URLClassLoader sharedApiClassLoader;

            if (lastClassLoader == null) {
                sharedApiClassLoader = new URLClassLoader(
                        new URL[]{file.toURI().toURL()}
                );
            } else {
                sharedApiClassLoader = new URLClassLoader(
                        new URL[]{file.toURI().toURL()},
                        lastClassLoader
                );
            }

            urlClassLoaders.add(sharedApiClassLoader);

        } catch (Exception e) {
            throw new OperationIncompleteException(e);
        }
    }

    private void initPreloadedResources() {
        List<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(ServiceFactoryInterface.class.getClassLoader());
        classLoaders.add(WorkflowRoutineFactoryInterface.class.getClassLoader());
        CompositeClassLoader compositeClassLoader = new CompositeClassLoader(classLoaders);
        urlClassLoaders.add(compositeClassLoader);

        // Same as previous
        //urlClassLoaders.add(ClassLoader.getSystemClassLoader());
    }
}
