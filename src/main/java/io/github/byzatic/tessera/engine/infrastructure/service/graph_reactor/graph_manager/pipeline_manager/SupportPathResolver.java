package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import java.nio.file.Path;

public class SupportPathResolver {

    private static final String NODE_PATH_VARIABLE = "${NODE_PATH}";
    private static final String PROJECT_GLOBAL_PATH_VARIABLE = "${PROJECT_GLOBAL_PATH}";

    private final Path nodeFileStoragePath;
    private final Path projectGlobalFileStoragePath;

    public SupportPathResolver(
            Path nodeFileStoragePath,
            Path projectGlobalFileStoragePath
    ) {
        this.nodeFileStoragePath = nodeFileStoragePath;
        this.projectGlobalFileStoragePath = projectGlobalFileStoragePath;
    }

    public String processTemplate(String templateString) {
        String result = resolvePath(
                templateString,
                NODE_PATH_VARIABLE,
                nodeFileStoragePath
        );

        return resolvePath(
                result,
                PROJECT_GLOBAL_PATH_VARIABLE,
                projectGlobalFileStoragePath
        );
    }

    private String resolvePath(
            String input,
            String variable,
            Path basePath
    ) {
        int variableIndex = input.indexOf(variable);

        if (variableIndex < 0) {
            return input;
        }

        String relativePath = input.replace(variable, "");

        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        return basePath
                .resolve(relativePath)
                .normalize()
                .toString();
    }
}