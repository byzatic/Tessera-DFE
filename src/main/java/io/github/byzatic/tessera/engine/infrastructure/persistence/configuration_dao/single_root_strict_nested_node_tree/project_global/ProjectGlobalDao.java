package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_global;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository.ProjectGlobalDaoInterface;

import java.nio.file.Path;

public class ProjectGlobalDao implements ProjectGlobalDaoInterface {
    private final static Logger logger = LoggerFactory.getLogger(ProjectGlobalDao.class);

    @Override
    public ProjectGlobal load(String projectName) throws OperationIncompleteException {
        try {
            Path projectsDirectory = Configuration.PROJECTS_DIR.resolve(projectName);
            ProjectGlobal projectGlobal = SupportProjectGlobalLoader.load(projectsDirectory.resolve("data").resolve("Global.json"));
            return projectGlobal;
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
