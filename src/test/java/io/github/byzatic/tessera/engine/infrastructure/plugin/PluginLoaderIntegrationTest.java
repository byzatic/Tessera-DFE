package io.github.byzatic.tessera.engine.infrastructure.plugin;

import io.github.byzatic.lib.configio.application.module.ModuleLoaderInterface;
import io.github.byzatic.lib.configio.application.loader.ProjectLoaderInterface;
import io.github.byzatic.lib.configio.application.service.ServiceLoaderInterface;
import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.infrastructure.factory.ModuleLoaderFactory;
import io.github.byzatic.lib.configio.infrastructure.factory.ProjectV1LoaderFactory;
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
        Path projectDirectory = Configuration.DATA_DIR
                .resolve("projects")
                .resolve(Configuration.PROJECT_NAME);
        assumeTrue(
                "Integration project is not available: " + projectDirectory,
                Files.isDirectory(projectDirectory)
        );

        ProjectLoaderInterface projectLoader = ProjectV1LoaderFactory.create();
        try (ProjectLoadResultDataObject loadedProject = projectLoader.load(projectDirectory)) {
            ProjectRepository projectRepository = new ProjectRepositoryImpl(loadedProject);
            try (ModuleLoaderInterface moduleLoader = ModuleLoaderFactory.create(
                     projectDirectory.resolve("modules").resolve("workflow_routines"),
                     projectRepository.getSharedResourcesClassLoader()
             );
             ServiceLoaderInterface serviceLoader = ServiceLoaderFactory.create(
                     projectDirectory.resolve("modules").resolve("services"),
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
}
