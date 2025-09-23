package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility service that ingests ZIP archives from a source path (either a directory or a single .zip file),
 * copies them into the destination directory, resolves name conflicts (delete or archive-with-timestamp),
 * and extracts them. On any failure, throws {@link OperationIncompleteException}.
 *
 * <p>Conflict handling:
 * <ul>
 *   <li>If a ZIP with the same name already exists in the destination, or the target extraction folder exists,
 *       the configured {@link ConflictPolicy} is applied.</li>
 *   <li>{@link ConflictPolicy#DELETE}: Existing file/folder is removed.</li>
 *   <li>{@link ConflictPolicy#ARCHIVE_WITH_TIMESTAMP}: Existing file/folder is moved or zipped under
 *       <code>destinationDir/archive/</code> with a timestamp suffix.</li>
 * </ul>
 *
 * <p>Security: extraction prevents "zip slip" by validating canonical paths.</p>
 */
public class ZipArchiveImporter {

    private static final Logger log = LoggerFactory.getLogger(ZipArchiveImporter.class);

    /**
     * How to resolve conflicts when target file/folder already exists.
     */
    public enum ConflictPolicy {
        /** Remove existing conflicting file(s)/folder(s). */
        DELETE,
        /** Move or zip existing conflicting file(s)/folder(s) into an "archive" area with a timestamp suffix. */
        ARCHIVE_WITH_TIMESTAMP
    }

    private final Path destinationDir;
    private final ConflictPolicy conflictPolicy;
    private final DateTimeFormatter timestampFormatter;
    private final Path archiveDir;

    /**
     * Create a new importer.
     *
     * @param destinationDir     Destination directory where zips will be copied and extracted.
     * @param conflictPolicy     Conflict resolution policy (delete or archive-with-timestamp).
     * @param timestampPattern   Timestamp pattern for archived names (e.g. "yyyyMMdd-HHmmss").
     *                           Used only when {@link ConflictPolicy#ARCHIVE_WITH_TIMESTAMP}. If {@code null},
     *                           defaults to "yyyyMMdd-HHmmss".
     * @throws OperationIncompleteException if destination directory cannot be created or verified.
     */
    public ZipArchiveImporter(Path destinationDir,
                              ConflictPolicy conflictPolicy,
                              String timestampPattern) throws OperationIncompleteException {
        Objects.requireNonNull(destinationDir, "destinationDir");
        Objects.requireNonNull(conflictPolicy, "conflictPolicy");

        this.destinationDir = destinationDir.toAbsolutePath().normalize();
        this.conflictPolicy = conflictPolicy;
        this.timestampFormatter = DateTimeFormatter.ofPattern(
                timestampPattern == null || timestampPattern.isBlank() ? "yyyyMMdd-HHmmss" : timestampPattern
        );
        this.archiveDir = this.destinationDir.resolve("archive");

        try {
            if (Files.notExists(this.destinationDir)) {
                log.debug("Destination directory does not exist, creating: {}", this.destinationDir);
                Files.createDirectories(this.destinationDir);
            }
            if (!Files.isDirectory(this.destinationDir)) {
                throw new OperationIncompleteException("Destination is not a directory: " + this.destinationDir);
            }
            if (conflictPolicy == ConflictPolicy.ARCHIVE_WITH_TIMESTAMP) {
                // Prepare archive dir eagerly to fail-fast if FS forbids it.
                if (Files.notExists(this.archiveDir)) {
                    log.debug("Archive directory does not exist, creating: {}", this.archiveDir);
                    Files.createDirectories(this.archiveDir);
                }
            }
        } catch (IOException e) {
            log.error("Failed to initialize ZipArchiveImporter: {}", e.getMessage(), e);
            throw new OperationIncompleteException("Failed to prepare destination/archive directories", e);
        }
    }

    /**
     * Ingest ZIPs from the given source path. If the path is a directory, all direct children
     * with extension ".zip" (case-insensitive) are processed (non-recursive).
     * If the path is a file, it must be a single ZIP file.
     *
     * <p>On any failure, throws {@link OperationIncompleteException} and stops processing.</p>
     *
     * @param sourcePath Path to a directory containing zip files (non-recursive), or to a single .zip file.
     * @throws OperationIncompleteException if any I/O or processing error occurs.
     */
    public void ingest(Path sourcePath) throws OperationIncompleteException {
        Objects.requireNonNull(sourcePath, "sourcePath");
        Path src = sourcePath.toAbsolutePath().normalize();
        log.debug("Starting ingestion from source: {}", src);

        try {
            final List<Path> zips = new ArrayList<>();

            if (Files.isDirectory(src)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(src, p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return Files.isRegularFile(p) && name.endsWith(".zip");
                })) {
                    for (Path p : ds) {
                        zips.add(p);
                    }
                }
            } else if (Files.isRegularFile(src) && src.getFileName().toString().toLowerCase().endsWith(".zip")) {
                zips.add(src);
            } else {
                throw new OperationIncompleteException("Source is neither a directory with zips nor a .zip file: " + src);
            }

            if (zips.isEmpty()) {
                log.debug("No ZIP files found at source: {}", src);
                return; // Nothing to do.
            }

            log.debug("Found {} ZIP file(s) to process.", zips.size());
            if (log.isTraceEnabled()) {
                zips.forEach(p -> log.trace("ZIP candidate: {}", p));
            }

            for (Path zip : zips) {
                processSingleZip(zip);
            }

            log.debug("Ingestion completed successfully from source: {}", src);
        } catch (OperationIncompleteException e) {
            // Already logged at the source of failure; rethrow.
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during ingestion: {}", e.getMessage(), e);
            throw new OperationIncompleteException("Unexpected error during ingestion", e);
        }
    }

    /**
     * Ingest exactly one ZIP file: resolve conflicts in the destination, copy the file,
     * and extract it. This method does NOT scan the parent directory; it processes only
     * the provided path.
     *
     * <p>On any failure, throws {@link OperationIncompleteException} and stops.</p>
     *
     * @param zipFile Path to a single .zip file (absolute or relative).
     * @throws OperationIncompleteException if the path is invalid, not a ZIP, or any I/O error occurs.
     */
    public void ingestSingle(Path zipFile) throws OperationIncompleteException {
        Objects.requireNonNull(zipFile, "zipFile");
        Path src = zipFile.toAbsolutePath().normalize();
        log.debug("Starting single-zip ingestion: {}", src);
        try {
            processSingleZip(src);
            log.debug("Single-zip ingestion completed successfully: {}", src);
        } catch (OperationIncompleteException e) {
            // Already logged at source; rethrow unchanged.
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during single-zip ingestion for {}: {}", src, e.getMessage(), e);
            throw new OperationIncompleteException("Unexpected error during single-zip ingestion", e);
        }
    }

    /**
     * Process a single ZIP file: resolve conflicts in destination, copy, and extract.
     */
    private void processSingleZip(Path sourceZip) throws OperationIncompleteException {
        log.debug("Processing ZIP: {}", sourceZip);
        if (!Files.isRegularFile(sourceZip)) {
            throw new OperationIncompleteException("Not a regular file: " + sourceZip);
        }

        String fileName = sourceZip.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".zip")) {
            throw new OperationIncompleteException("File is not a .zip: " + sourceZip);
        }

        String baseName = fileName.substring(0, fileName.length() - 4); // drop ".zip"
        Path targetZip = destinationDir.resolve(fileName);
        Path extractDir = destinationDir.resolve(baseName);

        // 1) Resolve conflicts for existing target zip and extraction dir.
        resolveConflictsIfAny(targetZip, extractDir);

        // 2) Copy ZIP into destination.
        copyZip(sourceZip, targetZip);

        // 3) Extract into destination/<baseName>
        extractZip(targetZip, extractDir);
    }

    /**
     * Resolve conflicts according to the configured policy.
     * - If target zip exists: delete it OR move/rename into archive with timestamp.
     * - If extraction directory exists: delete it recursively OR zip it into archive and remove original.
     */
    private void resolveConflictsIfAny(Path targetZip, Path extractDir) throws OperationIncompleteException {
        boolean zipExists = Files.exists(targetZip);
        boolean dirExists = Files.exists(extractDir);

        if (!zipExists && !dirExists) {
            log.trace("No conflicts for {} and {}", targetZip.getFileName(), extractDir.getFileName());
            return;
        }

        log.debug("Resolving conflicts for targetZip={} exists={}, extractDir={} exists={}, policy={}",
                targetZip, zipExists, extractDir, dirExists, conflictPolicy);

        try {
            if (conflictPolicy == ConflictPolicy.DELETE) {
                if (zipExists) {
                    log.debug("Deleting existing ZIP: {}", targetZip);
                    Files.delete(targetZip);
                }
                if (dirExists) {
                    log.debug("Deleting existing directory: {}", extractDir);
                    deleteRecursively(extractDir);
                }
            } else if (conflictPolicy == ConflictPolicy.ARCHIVE_WITH_TIMESTAMP) {
                String ts = LocalDateTime.now().format(timestampFormatter);

                if (zipExists) {
                    String archivedName = stripZipExt(targetZip.getFileName().toString()) + "." + ts + ".zip";
                    Path archivedZip = archiveDir.resolve(archivedName);
                    log.debug("Archiving existing ZIP: {} -> {}", targetZip, archivedZip);
                    Files.createDirectories(archiveDir);
                    Files.move(targetZip, archivedZip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }

                if (dirExists) {
                    String archivedName = extractDir.getFileName().toString() + "." + ts + ".zip";
                    Path archivedZip = archiveDir.resolve(archivedName);
                    log.debug("Zipping existing directory into archive: {} -> {}", extractDir, archivedZip);
                    Files.createDirectories(archiveDir);
                    zipDirectory(extractDir, archivedZip);
                    log.debug("Deleting original directory after archive: {}", extractDir);
                    deleteRecursively(extractDir);
                }
            } else {
                throw new IllegalStateException("Unknown conflictPolicy: " + conflictPolicy);
            }
        } catch (IOException e) {
            log.error("Failed to resolve conflicts for {} / {}: {}", targetZip, extractDir, e.getMessage(), e);
            throw new OperationIncompleteException("Failed to resolve conflicts in destination", e);
        }
    }

    private static String stripZipExt(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    /**
     * Copy source ZIP to destination (overwrite was already handled by conflict resolution).
     */
    private void copyZip(Path sourceZip, Path targetZip) throws OperationIncompleteException {
        log.debug("Copying ZIP to destination: {} -> {}", sourceZip, targetZip);
        try {
            Files.copy(sourceZip, targetZip, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to copy {} to {}: {}", sourceZip, targetZip, e.getMessage(), e);
            throw new OperationIncompleteException("Failed to copy ZIP to destination", e);
        }
    }

    /**
     * Extract the ZIP into the given directory, ensuring no zip-slip is possible.
     */
    private void extractZip(Path zipFile, Path extractDir) throws OperationIncompleteException {
        log.debug("Extracting ZIP {} into {}", zipFile, extractDir);
        try {
            Files.createDirectories(extractDir);

            Path canonicalRoot = extractDir.toRealPath();

            try (InputStream fis = Files.newInputStream(zipFile);
                 ZipInputStream zis = new ZipInputStream(fis)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = canonicalRoot.resolve(entry.getName()).normalize();

                    // Prevent zip slip: ensure target is under canonical root
                    if (!outPath.startsWith(canonicalRoot)) {
                        throw new OperationIncompleteException("Blocked zip entry outside target dir: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        log.trace("Creating directory from entry: {}", outPath);
                        Files.createDirectories(outPath);
                    } else {
                        // Ensure parent directories exist
                        Path parent = outPath.getParent();
                        if (parent != null && Files.notExists(parent)) {
                            Files.createDirectories(parent);
                        }
                        log.trace("Writing file from entry: {}", outPath);
                        try (OutputStream os = Files.newOutputStream(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = zis.read(buf)) >= 0) {
                                os.write(buf, 0, r);
                            }
                        }
                        // Optionally, set last modified time from ZIP entry if present
                        if (entry.getLastModifiedTime() != null) {
                            Files.setLastModifiedTime(outPath, entry.getLastModifiedTime());
                        }
                    }
                    zis.closeEntry();
                }
            }

            log.debug("Extraction completed: {}", extractDir);
        } catch (IOException e) {
            log.error("Extraction failed for {}: {}", zipFile, e.getMessage(), e);
            // Best-effort cleanup of incomplete extraction
            try {
                if (Files.exists(extractDir)) {
                    deleteRecursively(extractDir);
                }
            } catch (IOException cleanupEx) {
                log.error("Cleanup after extraction failure also failed for {}: {}", extractDir, cleanupEx.getMessage(), cleanupEx);
            }
            throw new OperationIncompleteException("Failed to extract ZIP: " + zipFile, e);
        }
    }

    /**
     * Zip a directory recursively into a .zip file. Overwrites the target if exists.
     */
    private static void zipDirectory(Path sourceDir, Path targetZip) throws IOException {
        Path root = sourceDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Not a directory: " + root);
        }
        Path parent = targetZip.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }

        try (OutputStream fos = Files.newOutputStream(targetZip, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = root.relativize(dir);
                    String entryName = rel.toString().replace('\\', '/');
                    if (!entryName.isEmpty()) {
                        // Ensure directory entries end with '/'
                        if (!entryName.endsWith("/")) entryName = entryName + "/";
                        ZipEntry de = new ZipEntry(entryName);
                        zos.putNextEntry(de);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = root.relativize(file);
                    String entryName = rel.toString().replace('\\', '/');
                    ZipEntry fe = new ZipEntry(entryName);
                    zos.putNextEntry(fe);
                    try (InputStream is = Files.newInputStream(file)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) >= 0) {
                            zos.write(buf, 0, r);
                        }
                    } catch (IOException io) {
                        throw new UncheckedIOException(io);
                    } finally {
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (UncheckedIOException wrap) {
            throw wrap.getCause();
        }
    }

    /**
     * Recursively delete a directory/file tree.
     */
    private static void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
                Files.delete(f);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
