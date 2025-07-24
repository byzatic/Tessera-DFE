package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.node.Project;

class SupportProjectValidation {
    private final static Logger logger = LoggerFactory.getLogger(SupportProjectValidation.class);
    private final String versionId = "v1.0.0";
    private final String versionType = "SingleRootStrictNestedNodeTree";
    private final String version = versionId+"-"+versionType;

    public void validate(Project configProject) throws OperationIncompleteException {
        try {
            String projectConfigVersion = configProject.getProjectConfigVersion();
            if (projectConfigVersion.equals(version)) {
                logger.debug("ConfigProject {} versionId {} versionType {}", configProject.getProjectName(), versionId, versionType);
            } else {
                String errMessage = "Unsupported configProject version " + projectConfigVersion;
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
