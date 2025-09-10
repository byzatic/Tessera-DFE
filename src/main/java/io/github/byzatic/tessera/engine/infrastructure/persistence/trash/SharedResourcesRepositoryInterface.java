package io.github.byzatic.tessera.engine.infrastructure.persistence.trash;

import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.ResourcesInterface;
import org.jetbrains.annotations.Nullable;

public interface SharedResourcesRepositoryInterface extends ResourcesInterface {
    @Nullable ClassLoader getSharedResourcesClassLoader();
}
