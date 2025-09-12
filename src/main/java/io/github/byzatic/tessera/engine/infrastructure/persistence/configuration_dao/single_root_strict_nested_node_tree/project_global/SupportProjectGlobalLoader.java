package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_global;

import com.google.gson.Gson;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.nio.file.Path;

class SupportProjectGlobalLoader {
    private final static Logger logger = LoggerFactory.getLogger(SupportProjectGlobalLoader.class);
    private final static Gson gson = new Gson();


    public static ProjectGlobal load(Path fileProjectGlobal) throws OperationIncompleteException {
        try {
            FileReader reader = new FileReader(fileProjectGlobal.toFile());
            ProjectGlobal projectGlobal = gson.fromJson(reader, ProjectGlobal.class);
            logger.debug("Loaded {} from {}", ProjectGlobal.class.getSimpleName(), fileProjectGlobal);
            logger.trace("Loaded {} from {} -> {}", ProjectGlobal.class.getSimpleName(), fileProjectGlobal, projectGlobal);
            return projectGlobal;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
