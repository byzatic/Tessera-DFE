package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectConfigReader {

    /**
     * Читает значение project_config_version из JSON файла.
     *
     * @param filePath путь до JSON файла
     * @return значение project_config_version или null, если его нет
     * @throws IOException если файл не найден или не удалось прочитать
     */
    public static String readProjectConfigVersion(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        String content = Files.readString(filePath);

        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        if (root.has("project_config_version")) {
            return root.get("project_config_version").getAsString();
        }

        return null;
    }
}
