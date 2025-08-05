package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeProjectGlobalRepositoryInterface;

public class JpaLikeProjectGlobalRepository implements JpaLikeProjectGlobalRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(JpaLikeProjectGlobalRepository.class);
    private final ProjectGlobalDaoInterface projectGlobalDao;
    private final String projectName;
    private ProjectGlobal projectGlobal = null;

    public JpaLikeProjectGlobalRepository(String projectName, ProjectGlobalDaoInterface projectGlobalDao) throws OperationIncompleteException {
        try {
            if (projectName == null) {
                String errMessage = "ProjectName can not be null";
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
            this.projectGlobalDao = projectGlobalDao;
            this.projectName = projectName;
            load();
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load() throws OperationIncompleteException {
        projectGlobal = projectGlobalDao.load(projectName);
    }

    @Override
    public ProjectGlobal getProjectGlobal() {
        logger.debug("Requested {}", ProjectGlobal.class.getSimpleName());
        logger.debug("Returns requested {}", ProjectGlobal.class.getSimpleName());
        logger.trace("Returns requested {} -> {}", ProjectGlobal.class.getSimpleName(), projectGlobal);
        return projectGlobal;
    }

    @Override
    public Boolean isStorageWithId(String storageId) {
        boolean result = Boolean.FALSE;
        for (StoragesItem storageItem : projectGlobal.getStorages()) {
            if (storageItem.getIdName().equals("storageId")) {
                result = Boolean.TRUE;
                break;
            }
        }
        return result;
    }

    @Override
    public void reload() throws OperationIncompleteException {
        try {
            projectGlobal = null;
            projectGlobal = projectGlobalDao.load(projectName);
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
