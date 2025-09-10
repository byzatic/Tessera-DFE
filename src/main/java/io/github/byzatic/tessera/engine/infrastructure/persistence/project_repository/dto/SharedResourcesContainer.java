package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class SharedResourcesContainer {
    private final List<ClassLoader> sharedResourcesUrlClassLoaders;

    public SharedResourcesContainer(@NotNull List<ClassLoader> sharedResourcesUrlClassLoaders) {
        this.sharedResourcesUrlClassLoaders = sharedResourcesUrlClassLoaders;
    }

    public synchronized @Nullable ClassLoader getSharedResourcesClassLoader() {
        ClassLoader lastUrlClassLoader = null;
        if (!sharedResourcesUrlClassLoaders.isEmpty()) {
            lastUrlClassLoader = sharedResourcesUrlClassLoaders.get(sharedResourcesUrlClassLoaders.size() - 1);
        }
        return lastUrlClassLoader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedResourcesContainer that = (SharedResourcesContainer) o;
        return Objects.equals(sharedResourcesUrlClassLoaders, that.sharedResourcesUrlClassLoaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedResourcesUrlClassLoaders);
    }

    @Override
    public String toString() {
        return "SharedResourcesContainer{" +
                "sharedResourcesUrlClassLoaders=" + sharedResourcesUrlClassLoaders +
                '}';
    }
}
