package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.shared_resources_dao;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.SharedResourcesDAOInterface;
import io.github.byzatic.tessera.service.service.ServiceFactoryInterface;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineFactoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class SharedResourcesDAO implements SharedResourcesDAOInterface {
    private final static Logger logger = LoggerFactory.getLogger(SharedResourcesDAO.class);

    @Override
    public List<ClassLoader> loadSharedResources(String projectName) {
        List<ClassLoader> urlClassLoaders = new ArrayList<>();
        return urlClassLoaders;
    }

    public void load(String projectName, List<ClassLoader> urlClassLoaders) {
        try {
            initPreloadedResources(urlClassLoaders);

            File dir = Configuration.PROJECTS_DIR.resolve(projectName).resolve("modules").resolve("shared").toFile();

            if (!dir.exists())
                throw new OperationIncompleteException("Shared resources directory not exists! (" + dir + ")");

            File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    logger.debug("Found jar: " + jar.getAbsolutePath());
                    loadResource(jar, urlClassLoaders);
                    logger.debug("Loaded jar: " + jar.getAbsolutePath());
                }
            } else {
                logger.debug("No jars found or directory not valid");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadResource(File file, List<ClassLoader> urlClassLoaders) throws OperationIncompleteException {
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

    private void initPreloadedResources(List<ClassLoader> urlClassLoaders) {
        List<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(ServiceFactoryInterface.class.getClassLoader());
        classLoaders.add(WorkflowRoutineFactoryInterface.class.getClassLoader());
        CompositeClassLoader compositeClassLoader = new CompositeClassLoader(classLoaders);
        urlClassLoaders.add(compositeClassLoader);

        // Same as previous
        //urlClassLoaders.add(ClassLoader.getSystemClassLoader());
    }
}
