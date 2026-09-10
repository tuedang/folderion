package dev.folderion.bucket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Open handle to a bucket directory (one record type).
 */
public final class Bucket {

    private static final Pattern HISTORY_DIR_NAME = Pattern.compile("^.+_v[1-9][0-9]*$");

    private final Path root;
    private final BucketConfig config;
    private final RecordLayoutSchema schema;
    private final Pattern idPattern;

    Bucket(Path root, BucketConfig config, RecordLayoutSchema schema) {
        this.root = root.toAbsolutePath().normalize();
        this.config = config;
        this.schema = schema;
        this.idPattern = config.getIdPattern() == null || config.getIdPattern().isBlank()
                ? null
                : Pattern.compile(config.getIdPattern());
    }

    public Path root() {
        return root;
    }

    public BucketConfig config() {
        return config;
    }

    public RecordLayoutSchema schema() {
        return schema;
    }

    public Path recordsDir() {
        return root.resolve(config.getRecordsDir());
    }

    public Path recordDir(String id) {
        validateId(id);
        return recordsDir().resolve(id);
    }

    /**
     * Lean history nested dir: {@code records/{id}/{id}_v{n}/} ({@code n} starts at 1 = newest archive).
     */
    public Path historyVersionDir(String id, int version) {
        validateId(id);
        if (version < 1) {
            throw new FolderionException("History version must be >= 1: " + version);
        }
        return recordDir(id).resolve(versionFolderName(id, version));
    }

    public boolean exists(String id) {
        return Files.isDirectory(recordDir(id));
    }

    public Optional<JsonNode> readRecord(String id) {
        Path file = recordDir(id).resolve(schema.path("record"));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Json.readTree(file));
    }

    /**
     * Existing lean history version numbers (ascending). {@code 1} = newest archive.
     */
    public List<Integer> listHistoryVersions(String id) {
        validateId(id);
        int max = schema.getMaxHistoryVersions();
        if (max < 1) {
            return List.of();
        }
        List<Integer> versions = new ArrayList<>();
        for (int v = 1; v <= max; v++) {
            if (Files.isDirectory(historyVersionDir(id, v))) {
                versions.add(v);
            }
        }
        return List.copyOf(versions);
    }

    public Optional<JsonNode> readHistoryRecord(String id, int version) {
        Path file = historyVersionDir(id, version).resolve(schema.path("record"));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(Json.readTree(file));
    }

    public CommitResult commit(WritePlan plan) {
        Objects.requireNonNull(plan, "plan");
        validateId(plan.id());
        validatePlanAgainstBucket(plan);

        ObjectNode record = plan.record().deepCopy();
        enrichIdentity(record, plan);

        String contentFp = Fingerprints.contentFingerprint(record, schema.getFingerprintFields());
        List<String> mediaDigests = collectMediaDigests(plan);
        String mediaFp = mediaDigests.isEmpty() ? null : Fingerprints.mediaFingerprint(mediaDigests);

        Path finalDir = recordDir(plan.id());
        boolean existed = Files.isDirectory(finalDir);

        if (existed) {
            Optional<RecordIntegrity> previous = readIntegrity(finalDir);
            if (previous.isPresent()
                    && contentFp.equals(previous.get().getContentSha256())
                    && Objects.equals(mediaFp, previous.get().getMediaSha256())) {
                touchLastSeen(finalDir);
                return new CommitResult(CommitResult.Status.UNCHANGED, finalDir, contentFp, mediaFp);
            }
        }

        record.set("integrity", Json.valueToTree(new RecordIntegrity(contentFp, mediaFp)));

        Path staging = root.resolve(".staging").resolve(plan.id() + "-" + System.nanoTime());
        try {
            Files.createDirectories(staging);
            writeRecordTree(staging, plan, record);
            if (existed) {
                preserveAndMaybeRotateNestedHistory(plan.id(), finalDir, staging, record);
            }
            Io.moveReplace(staging, finalDir);
        } catch (RuntimeException e) {
            Io.deleteRecursively(staging);
            throw e;
        } catch (IOException e) {
            Io.deleteRecursively(staging);
            throw new FolderionException("Failed to stage record " + plan.id(), e);
        }

        return new CommitResult(
                existed ? CommitResult.Status.UPDATED : CommitResult.Status.CREATED,
                finalDir,
                contentFp,
                mediaFp);
    }

    /**
     * Copy nested {@code {id}_vN} into staging so HEAD replace does not wipe them; when
     * {@code historyFields} change, rotate and archive prior HEAD {@code record.json} into
     * {@code {id}_v1} (lean — no media). Drops versions beyond max.
     */
    private void preserveAndMaybeRotateNestedHistory(
            String id, Path finalDir, Path staging, ObjectNode newRecord) {
        int max = schema.getMaxHistoryVersions();
        if (max < 1) {
            return;
        }

        List<String> historyFields = schema.getHistoryFields();
        Path oldRecordFile = finalDir.resolve(schema.path("record"));
        boolean shouldArchive = false;
        if (historyFields != null && !historyFields.isEmpty() && Files.isRegularFile(oldRecordFile)) {
            JsonNode oldRecord = Json.readTree(oldRecordFile);
            String oldHistoryFp = Fingerprints.contentFingerprint(oldRecord, historyFields);
            String newHistoryFp = Fingerprints.contentFingerprint(newRecord, historyFields);
            shouldArchive = !oldHistoryFp.equals(newHistoryFp);
        }

        for (int v = 1; v <= max; v++) {
            Path from = finalDir.resolve(versionFolderName(id, v));
            if (Files.isDirectory(from)) {
                Io.copyRecursively(from, staging.resolve(versionFolderName(id, v)));
            }
        }

        if (!shouldArchive) {
            return;
        }

        Path drop = staging.resolve(versionFolderName(id, max));
        if (Files.exists(drop)) {
            Io.deleteRecursively(drop);
        }
        for (int v = max - 1; v >= 1; v--) {
            Path from = staging.resolve(versionFolderName(id, v));
            if (Files.isDirectory(from)) {
                Io.moveReplace(from, staging.resolve(versionFolderName(id, v + 1)));
            }
        }

        Path archiveFile = staging.resolve(versionFolderName(id, 1)).resolve(schema.path("record"));
        try {
            Files.createDirectories(archiveFile.getParent());
            Files.copy(oldRecordFile, archiveFile);
        } catch (IOException e) {
            throw new FolderionException("Failed to archive lean history " + id + "_v1", e);
        }
    }

    static String versionFolderName(String id, int version) {
        return id + "_v" + version;
    }

    public Stream<String> listRecordIds() {
        Path dir = recordsDir();
        if (!Files.isDirectory(dir)) {
            return Stream.empty();
        }
        try {
            return Files.list(dir)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !name.startsWith("."))
                    .filter(name -> !HISTORY_DIR_NAME.matcher(name).matches())
                    .sorted();
        } catch (IOException e) {
            throw new FolderionException("Failed to list records in " + dir, e);
        }
    }

    private void writeRecordTree(Path staging, WritePlan plan, ObjectNode record) {
        Json.write(staging.resolve(schema.path("record")), record);

        String readmePath = schema.pathOrDefault("readme", "README.md");
        if (plan.readmeMarkdown() != null) {
            Io.writeText(staging.resolve(readmePath), plan.readmeMarkdown());
        }

        if (plan.sourceUrl() != null) {
            MediaSlot urlSlot = findUrlSlot().orElse(null);
            String urlPath = urlSlot != null ? urlSlot.getPath() : schema.pathOrDefault("source_url", "source/original.url");
            Io.writeText(staging.resolve(urlPath), plan.sourceUrl().trim() + "\n");
        }

        for (var entry : plan.mediaBySlot().entrySet()) {
            MediaSlot slot = schema.requireMedia(entry.getKey());
            if (slot.getKind() != MediaKind.IMAGE_SET) {
                throw new FolderionException("Slot " + entry.getKey() + " is not IMAGE_SET");
            }
            writeImageSet(staging, slot, entry.getValue());
        }

        for (var entry : plan.singleBlobsBySlot().entrySet()) {
            MediaSlot slot = schema.requireMedia(entry.getKey());
            if (slot.getPath() == null) {
                throw new FolderionException("Slot " + entry.getKey() + " has no path");
            }
            Io.writeBytes(staging.resolve(slot.getPath()), entry.getValue());
        }

        Io.writeText(staging.resolve(".state/last_seen_at"), Instant.now().toString() + "\n");
        Io.writeText(staging.resolve(".state/content_fingerprint"),
                record.get("integrity").get("contentSha256").asText() + "\n");
        JsonNode mediaNode = record.get("integrity").get("mediaSha256");
        if (mediaNode != null && !mediaNode.isNull()) {
            Io.writeText(staging.resolve(".state/media_fingerprint"), mediaNode.asText() + "\n");
        }
    }

    private void writeImageSet(Path staging, MediaSlot slot, List<MediaBlob> images) {
        Path dir = staging.resolve(slot.getDir());
        ImageManifest manifest = new ImageManifest();
        List<ImageManifest.Item> items = new ArrayList<>();
        int index = 1;
        for (MediaBlob blob : images) {
            String fileName = blob.fileName();
            Io.writeBytes(dir.resolve(fileName), blob.bytes());
            ImageManifest.Item item = new ImageManifest.Item();
            item.setFile(fileName);
            item.setSourceUrl(blob.sourceUrl());
            item.setSha256(blob.sha256());
            item.setOrder(blob.order() != null ? blob.order() : index);
            items.add(item);
            index++;
        }
        manifest.setItems(items);
        manifest.setCount(items.size());
        if (slot.getManifest() != null) {
            Json.write(staging.resolve(slot.getManifest()), manifest);
        }
    }

    private List<String> collectMediaDigests(WritePlan plan) {
        List<String> digests = new ArrayList<>();
        for (List<MediaBlob> blobs : plan.mediaBySlot().values()) {
            for (MediaBlob blob : blobs) {
                digests.add(blob.sha256());
            }
        }
        for (byte[] bytes : plan.singleBlobsBySlot().values()) {
            digests.add(Fingerprints.sha256Hex(bytes));
        }
        return digests;
    }

    private void enrichIdentity(ObjectNode record, WritePlan plan) {
        record.put("id", plan.id());
        record.put("record_type", config.getRecordType());
        record.put("layout_version", config.getLayoutVersion());
        if (plan.sourceUrl() != null) {
            ObjectNode source = objectChild(record, "source");
            source.put("url", plan.sourceUrl());
        }
        ObjectNode crawl = objectChild(record, "crawl");
        crawl.put("fetched_at", Instant.now().toString());
        crawl.put("layout_version", config.getLayoutVersion());
    }

    private void validatePlanAgainstBucket(WritePlan plan) {
        JsonNode type = plan.record().get("record_type");
        if (type != null && !type.asText().equals(config.getRecordType())) {
            throw new FolderionException("record_type mismatch: plan=" + type.asText()
                    + " bucket=" + config.getRecordType());
        }
        for (String key : plan.mediaBySlot().keySet()) {
            schema.requireMedia(key);
        }
        for (String key : plan.singleBlobsBySlot().keySet()) {
            schema.requireMedia(key);
        }
    }

    private void validateId(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\") || id.contains("..")) {
            throw new FolderionException("Invalid record id: " + id);
        }
        if (HISTORY_DIR_NAME.matcher(id).matches()) {
            throw new FolderionException("Record id must not use history suffix _vN: " + id);
        }
        if (idPattern != null && !idPattern.matcher(id).matches()) {
            throw new FolderionException("Record id does not match pattern " + idPattern + ": " + id);
        }
    }

    private Optional<MediaSlot> findUrlSlot() {
        return schema.getMedia().stream().filter(m -> m.getKind() == MediaKind.URL_FILE).findFirst();
    }

    private Optional<RecordIntegrity> readIntegrity(Path recordDir) {
        Path recordFile = recordDir.resolve(schema.path("record"));
        if (!Files.isRegularFile(recordFile)) {
            return Optional.empty();
        }
        JsonNode tree = Json.readTree(recordFile);
        JsonNode integrity = tree.get("integrity");
        if (integrity == null || integrity.isNull()) {
            return Optional.empty();
        }
        return Optional.of(Json.mapper().convertValue(integrity, RecordIntegrity.class));
    }

    private void touchLastSeen(Path recordDir) {
        Io.writeText(recordDir.resolve(".state/last_seen_at"), Instant.now().toString() + "\n");
    }

    private static ObjectNode objectChild(ObjectNode parent, String field) {
        JsonNode existing = parent.get(field);
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return parent.putObject(field);
    }
}
