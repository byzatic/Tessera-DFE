package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import java.nio.file.Path;

public class SupportPathResolver {

    private final Path nodeFileStoragePath;
    private final Path projectGlobalFileStoragePath;

    public SupportPathResolver(Path nodeFileStoragePath, Path projectGlobalFileStoragePath) {
        this.nodeFileStoragePath = nodeFileStoragePath;
        this.projectGlobalFileStoragePath = projectGlobalFileStoragePath;
    }

    public String processTemplate(String templateString) {
        final String nodePathVariable = "${NODE_PATH}";
        final String projectGlobalVariable = "${PROJECT_GLOBAL_PATH}";
        String finalString = null;
        finalString = resolvePath(templateString, nodePathVariable, nodeFileStoragePath);
        finalString = resolvePath(finalString, projectGlobalVariable, projectGlobalFileStoragePath);
        return finalString;
    }

    public String resolvePath(String input, String variable, Path resolvePath) {
        String newInput = String.copyValueOf(input.toCharArray());
        if (!newInput.contains(variable)) {
            return input;
        }

        // Удаляем переменную из строки и оставляем относительный путь
        String relativePath = newInput.replace(variable, "");
        // Убираем ведущий слэш, если есть
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        // Собираем финальный путь
        Path resolvedPath = resolvePath.resolve(relativePath).normalize();

        return resolvedPath.toString();
    }

}
