package io.github.byzatic.tessera.engine.domain.repository;

import org.jetbrains.annotations.Nullable;

public interface SharedResourcesRepositoryInterface {
    @Nullable ClassLoader getSharedResourcesClassLoader();
}
