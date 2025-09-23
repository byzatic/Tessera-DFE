package io.github.byzatic.tessera.engine.infrastructure.config.reader;

/** Immutable-ish DTO; make setters/getters as needed. */
public final class TesseraDfeConfig {
    public TesseraDfeConfig.ProjectConfig project;            // may be null
    public TesseraDfeConfig.ProjectsDataConfig projectsData;  // may be null

    public static final class ProjectConfig {
        public String name;                       // may be null
        public String calculationCronCycle;       // required if project present
        public TesseraDfeConfig.StoragesConfig storages;           // may be null
    }

    public static final class StoragesConfig {
        public Boolean initializeByRequest;       // may be null
    }

    public static final class ProjectsDataConfig {
        public TesseraDfeConfig.DirWithPathConfig archiveDir;      // may be null
        public TesseraDfeConfig.RuntimeDirConfig runtimeDir;       // may be null
    }

    public static final class DirWithPathConfig {
        public String path;                       // required if archiveDir present
    }

    public static final class RuntimeDirConfig {
        public String path;                       // required if runtimeDir present
        public TesseraDfeConfig.RotationConfig rotation;           // may be null
    }

    public static final class RotationConfig {
        public Boolean enabled;                   // may be null
        public String timestampPattern;           // may be null
    }
}