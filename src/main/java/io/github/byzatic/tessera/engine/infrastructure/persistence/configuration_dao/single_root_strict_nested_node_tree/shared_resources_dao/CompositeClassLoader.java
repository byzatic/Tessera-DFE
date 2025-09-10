package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.shared_resources_dao;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

class CompositeClassLoader extends ClassLoader {
    private final List<ClassLoader> delegates;

    public CompositeClassLoader(List<ClassLoader> delegates) {
        super(null); // отключаем стандартное родительское делегирование
        this.delegates = delegates;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader cl : delegates) {
            try {
                return cl.loadClass(name);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader cl : delegates) {
            URL url = cl.getResource(name);
            if (url != null) return url;
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Vector<URL> resources = new Vector<>();
        for (ClassLoader cl : delegates) {
            resources.addAll(Collections.list(cl.getResources(name)));
        }
        return resources.elements();
    }
}
