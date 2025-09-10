package io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader;

import java.util.List;

public interface SharedResourcesDAOInterface {
    List<ClassLoader> loadSharedResources(String projectName);
}
