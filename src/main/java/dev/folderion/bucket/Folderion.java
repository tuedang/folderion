package dev.folderion.bucket;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for filesystem-first record storage.
 */
public final class Folderion {

    private Folderion() {
    }

    public static Folderion create() {
        return new Folderion();
    }

    /**
     * Create a new bucket directory with bucket.json + schema file.
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
        try {
            Files.createDirectories(root.resolve(config.getRecordsDir()));
            Files.createDirectories(root.resolve("schemas"));
        } catch (Exception e) {
            throw new FolderionException("Failed to create bucket dirs: " + root, e);
        }

        Path schemaPath = root.resolve(config.getSchema());
        Json.write(schemaPath, schema);
        Json.write(root.resolve("bucket.json"), config);
        return open(root);
    }

    /**
     * Open an existing bucket (reads bucket.json + schema).
     */
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
        return new Bucket(root, config, schema);
    }
}
