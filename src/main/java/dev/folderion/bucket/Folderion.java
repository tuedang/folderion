package dev.folderion.bucket;

import io.ocfl.api.OcflRepository;
import io.ocfl.core.OcflRepositoryBuilder;
import io.ocfl.core.extension.storage.layout.config.FlatLayoutConfig;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for filesystem-first record storage backed by OCFL (flat layout).
 *
 * <p>The bucket folder <em>is</em> the OCFL storage root so record objects live beside Folderion
 * meta (OCFL requires objects under the storage root — they cannot sit in a separate tree):
 *
 * <pre>
 *   buckets/
 *     centris/                      ← bucket meta + OCFL storage root
 *       bucket.json
 *       schemas/…
 *       0=ocfl_1.1                  ← required (NAMASTE)
 *       ocfl_layout.json            ← required
 *       centris-27481461/           ← record data + versions
 *         inventory.json
 *         v1/content/…
 *     centris-work/                 ← OCFL workspace (sibling)
 * </pre>
 *
 * Spec markdown copied by ocfl-java is stripped after init (not required at runtime).
 */
public final class Folderion {

    private Folderion() {
    }

    public static Folderion create() {
        return new Folderion();
    }

    /** {@code buckets/centris} → {@code buckets/centris-work}. */
    public static Path workDirFor(Path bucketRoot) {
        Path root = bucketRoot.toAbsolutePath().normalize();
        return root.resolveSibling(root.getFileName().toString() + "-work");
    }

    /**
     * Create a new bucket: Folderion meta + OCFL flat objects under {@code bucketRoot}.
     */
    public Bucket init(Path bucketRoot, BucketConfig config, RecordLayoutSchema schema) {
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(schema, "schema");
        schema.validate();

        if (!config.getRecordType().equals(schema.getRecordType())) {
            throw new FolderionException("bucket.recordType must match schema.recordType");
        }
        if (!config.getLayoutVersion().equals(schema.getLayoutVersion())) {
            throw new FolderionException("bucket.layoutVersion must match schema.layoutVersion");
        }

        Path root = bucketRoot.toAbsolutePath().normalize();
        Path workDir = workDirFor(root);
        try {
            Files.createDirectories(root);
            Files.createDirectories(workDir);
        } catch (Exception e) {
            throw new FolderionException("Failed to create bucket dirs: " + root, e);
        }

        // Empty OCFL root first, then Folderion meta files.
        OcflRepository repository = openRepository(root, workDir);
        stripNonEssentialOcflRootFiles(root);

        try {
            Files.createDirectories(root.resolve("schemas"));
        } catch (Exception e) {
            repository.close();
            throw new FolderionException("Failed to create schemas dir: " + root, e);
        }

        Json.write(root.resolve(config.getSchema()), schema);
        Json.write(root.resolve("bucket.json"), config);

        return new Bucket(root, root, workDir, config, schema, repository);
    }

    public Bucket open(Path bucketRoot) {
        Path root = bucketRoot.toAbsolutePath().normalize();
        Path bucketJson = root.resolve("bucket.json");
        if (!Files.isRegularFile(bucketJson)) {
            throw new FolderionException("Not a folderion bucket (missing bucket.json): " + root);
        }
        BucketConfig config = Json.read(bucketJson, BucketConfig.class);
        Path schemaPath = root.resolve(config.getSchema());
        if (!Files.isRegularFile(schemaPath)) {
            throw new FolderionException("Schema not found: " + schemaPath);
        }
        RecordLayoutSchema schema = Json.read(schemaPath, RecordLayoutSchema.class);
        schema.validate();

        Path workDir = workDirFor(root);
        try {
            Files.createDirectories(workDir);
        } catch (Exception e) {
            throw new FolderionException("Failed to ensure work dir: " + workDir, e);
        }

        OcflRepository repository = openRepository(root, workDir);
        stripNonEssentialOcflRootFiles(root);
        return new Bucket(root, root, workDir, config, schema, repository);
    }

    static OcflRepository openRepository(Path storageRoot, Path workDir) {
        return new OcflRepositoryBuilder()
                .defaultLayoutConfig(new FlatLayoutConfig())
                .storage(storage -> storage.fileSystem(storageRoot))
                .workDir(workDir)
                .prettyPrintJson()
                .build();
    }

    /** Remove ocfl-java copied {@code *.md} specs; keep namaste + {@code ocfl_layout.json}. */
    static void stripNonEssentialOcflRootFiles(Path ocflRoot) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(ocflRoot)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md")) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException e) {
            throw new FolderionException("Failed to clean OCFL root docs: " + ocflRoot, e);
        }
    }
}
