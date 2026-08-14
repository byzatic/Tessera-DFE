package io.github.byzatic.tessera.engine.infrastructure.plugin;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.lib.configio.infrastructure.factory.ModuleLoaderFactory;
import io.github.byzatic.lib.configio.infrastructure.factory.ServiceLoaderFactory;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.repository.ProjectRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectRepositoryImpl;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class PluginLoaderIntegrationTest {

    @Test
    public void shouldDiscoverProjectModulesAndServicesThroughConfigIoLibrary()
            throws Exception {
        Path projectDirectory = Configuration.PROJECTS_DIR.resolve(
                Configuration.PROJECT_NAME
        );
        assumeTrue(
                "Integration project is not available: " + projectDirectory,
                Files.isDirectory(projectDirectory)
        );

        ProjectRepository projectRepository = new ProjectRepositoryImpl(
                Configuration.PROJECT_NAME
        );
        projectRepository.load();

        try (ModuleLoaderInterface moduleLoader = ModuleLoaderFactory.create(
                     Configuration.PROJECT_WORKFLOW_ROUTINES_PATH,
                     projectRepository.getSharedResourcesClassLoader()
             );
             ServiceLoaderInterface serviceLoader = ServiceLoaderFactory.create(
                     Configuration.PROJECT_SERVICES_PATH,
                     projectRepository.getSharedResourcesClassLoader()
             )) {
            assertEquals(4, moduleLoader.getAvailableModuleNames().size());
            assertTrue(
                    moduleLoader.getAvailableModuleNames()
                            .contains("GetDataWorkflowRoutine")
            );
            assertEquals(1, serviceLoader.getAvailableServiceNames().size());
            assertTrue(
                    serviceLoader.getAvailableServiceNames()
                            .contains("PrometheusExportService")
            );
        }
    }
}
