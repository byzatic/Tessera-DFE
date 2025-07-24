package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_global_dao;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;

import java.io.FileReader;
import java.nio.file.Path;

class SupportNodeGlobalLoader {
    private final static Logger logger = LoggerFactory.getLogger(SupportNodeGlobalLoader.class);
    private final static Gson gson = new Gson();

    public static NodeGlobal load(Path fileNodeGlobal) throws OperationIncompleteException {
        try {
            FileReader reader = new FileReader(fileNodeGlobal.toFile());
            NodeGlobal nodeGlobal = gson.fromJson(reader, NodeGlobal.class);
            logger.debug("Loaded {} from {}", NodeGlobal.class.getSimpleName(), fileNodeGlobal);
            logger.trace("Loaded {} from {} -> {}", NodeGlobal.class.getSimpleName(), fileNodeGlobal, nodeGlobal);
            return nodeGlobal;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
