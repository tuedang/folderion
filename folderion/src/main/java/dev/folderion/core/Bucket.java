package dev.folderion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ocfl.api.OcflOption;
import io.ocfl.api.OcflRepository;
import io.ocfl.api.model.ObjectDetails;
import io.ocfl.api.model.ObjectVersionId;
import io.ocfl.api.model.OcflObjectVersion;
import io.ocfl.api.model.OcflObjectVersionFile;
import io.ocfl.api.model.VersionInfo;
import io.ocfl.api.model.VersionNum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Open handle to a Folderion bucket backed by an {@link OcflRepository}.
 *
 * <p>Each record id is one OCFL object. Commits that change content/media create a new immutable
 * OCFL version ({@code v1}, {@code v2}, …). Identical payloads return {@link CommitResult.Status#UNCHANGED}
 * without a new version.
 */
public final class Bucket implements AutoCloseable {

    private final Path root;
    private final Path ocflRoot;
    private final Path workDir;
    private final BucketConfig config;
    private final RecordLayoutSchema schema;
    private final OcflRepository repository;
    private final Pattern idPattern;

    Bucket(
            Path root,
            Path ocflRoot,
            Path workDir,
            BucketConfig config,
            RecordLayoutSchema schema,
            OcflRepository repository) {
        this.root = root.toAbsolutePath().normalize();
        this.ocflRoot = ocflRoot.toAbsolutePath().normalize();
        this.workDir = workDir.toAbsolutePath().normalize();
        this.config = config;
        this.schema = schema;
        this.repository = repository;
        this.idPattern = config.getIdPattern() == null || config.getIdPattern().isBlank()
                ? null
                : Pattern.compile(config.getIdPattern());
    }

    public Path root() {
        return root;
    }

    /** OCFL storage root — same as {@link #root()} for this layout (data lives under the bucket). */
    public Path ocflRoot() {
        return ocflRoot;
    }

    public Path workDir() {
        return workDir;
    }

    public BucketConfig config() {
        return config;
    }

    public RecordLayoutSchema schema() {
        return schema;
    }

    public OcflRepository repository() {
        return repository;
    }

    /**
     * OCFL object root under the bucket, e.g. {@code buckets/centris/centris-27481461/}.
     */
    public Path objectDir(String id) {
        validateId(id);
        return root.resolve(objectId(id));
    }

    /**
     * On-disk directory of HEAD logical files: {@code {bucket}/{bucketId}-{id}/vN/content/}.
     */
    public Path recordDir(String id) {
        return headContentDir(id);
    }

    public Path headContentDir(String id) {
        validateId(id);
        if (!repository.containsObject(objectId(id))) {
            throw new FolderionException("Unknown record: " + id);
        }
        VersionNum head = repository.describeObject(objectId(id)).getHeadVersionNum();
        return objectDir(id).resolve(head.toString()).resolve("content");
    }

    public boolean exists(String id) {
        validateId(id);
        return repository.containsObject(objectId(id));
    }

    public Optional<JsonNode> readRecord(String id) {
        return readLogicalJson(id, null, schema.path("record"));
    }

    /**
     * OCFL version numbers present for the object (ascending, {@code 1} = oldest).
     */
    public List<Integer> listVersions(String id) {
        validateId(id);
        if (!repository.containsObject(objectId(id))) {
            return List.of();
        }
        ObjectDetails details = repository.describeObject(objectId(id));
        return details.getVersionMap().keySet().stream()
                .sorted(Comparator.naturalOrder())
                .map(v -> Math.toIntExact(v.getVersionNum()))
                .toList();
    }

    /** @deprecated use {@link #listVersions(String)} */
    @Deprecated
    public List<Integer> listHistoryVersions(String id) {
        return listVersions(id);
    }

    public Optional<JsonNode> readVersionRecord(String id, int version) {
        validateId(id);
        if (version < 1) {
            throw new FolderionException("Version must be >= 1: " + version);
        }
        return readLogicalJson(id, VersionNum.fromInt(version), schema.path("record"));
    }

    /** @deprecated use {@link #readVersionRecord(String, int)} */
    @Deprecated
    public Optional<JsonNode> readHistoryRecord(String id, int version) {
        return readVersionRecord(id, version);
    }

    public Optional<String> readLogicalText(String id, String logicalPath) {
        return readLogicalStream(id, null, logicalPath).map(in -> {
            try (in) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new FolderionException("Failed to read " + logicalPath + " for " + id, e);
            }
        });
    }

    public Optional<JsonNode> readLogicalJson(String id, String logicalPath) {
        return readLogicalJson(id, null, logicalPath);
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

        String oid = objectId(plan.id());
        boolean existed = repository.containsObject(oid);

        if (existed) {
            Optional<RecordIntegrity> previous = readIntegrityFromHead(plan.id());
            if (previous.isPresent()
                    && contentFp.equals(previous.get().getContentSha256())
                    && Objects.equals(mediaFp, previous.get().getMediaSha256())) {
                return new CommitResult(CommitResult.Status.UNCHANGED, headContentDir(plan.id()), contentFp, mediaFp,
                        headVersionNum(plan.id()));
            }
        }

        record.set("integrity", Json.valueToTree(new RecordIntegrity(contentFp, mediaFp)));

        Path staging = workDir.resolve("folderion-stage").resolve(plan.id() + "-" + System.nanoTime());
        ObjectVersionId writtenId;
        try {
            Files.createDirectories(staging);
            writeRecordTree(staging, plan, record);
            VersionInfo info = new VersionInfo()
                    .setMessage(existed ? "folderion update" : "folderion create")
                    .setCreated(java.time.OffsetDateTime.now());

            if (!existed) {
                writtenId = repository.putObject(ObjectVersionId.head(oid), staging, info);
            } else {
                writtenId = repository.updateObject(ObjectVersionId.head(oid), info, updater -> {
                    updater.clearVersionState();
                    updater.addPath(staging, OcflOption.OVERWRITE);
                });
            }
        } catch (RuntimeException e) {
            Io.deleteRecursively(staging);
            throw e;
        } catch (Exception e) {
            Io.deleteRecursively(staging);
            throw new FolderionException("Failed to commit OCFL object " + oid, e);
        } finally {
            Io.deleteRecursively(staging);
        }

        int version = writtenId.getVersionNum() != null
                ? Math.toIntExact(writtenId.getVersionNum().getVersionNum())
                : headVersionNum(plan.id());
        return new CommitResult(
                existed ? CommitResult.Status.UPDATED : CommitResult.Status.CREATED,
                headContentDir(plan.id()),
                contentFp,
                mediaFp,
                version);
    }

    public Stream<String> listRecordIds() {
        String prefix = objectIdPrefix();
        return repository.listObjectIds()
                .filter(oid -> oid.startsWith(prefix))
                .map(oid -> oid.substring(prefix.length()))
                .sorted();
    }

    @Override
    public void close() {
        repository.close();
    }

    private Optional<JsonNode> readLogicalJson(String id, VersionNum version, String logicalPath) {
        return readLogicalStream(id, version, logicalPath).map(in -> {
            try (in) {
                return Json.readTree(in);
            } catch (IOException e) {
                throw new FolderionException("Failed to read JSON " + logicalPath + " for " + id, e);
            }
        });
    }

    private Optional<InputStream> readLogicalStream(String id, VersionNum version, String logicalPath) {
        validateId(id);
        String oid = objectId(id);
        if (!repository.containsObject(oid)) {
            return Optional.empty();
        }
        ObjectVersionId versionId = version == null
                ? ObjectVersionId.head(oid)
                : ObjectVersionId.version(oid, version);
        OcflObjectVersion objectVersion = repository.getObject(versionId);
        if (!objectVersion.containsFile(logicalPath)) {
            return Optional.empty();
        }
        OcflObjectVersionFile file = objectVersion.getFile(logicalPath);
        return Optional.of(file.getStream());
    }

    private void writeRecordTree(Path staging, WritePlan plan, ObjectNode record) {
        Json.write(staging.resolve(schema.path("record")), record);

        if (plan.sourceUrl() != null) {
            MediaSlot urlSlot = findUrlSlot().orElse(null);
            String urlPath = urlSlot != null ? urlSlot.getPath() : schema.pathOrDefault("source_url", "original.url");
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
        if (idPattern != null && !idPattern.matcher(id).matches()) {
            throw new FolderionException("Record id does not match pattern " + idPattern + ": " + id);
        }
    }

    /** OCFL object id: {@code {bucketId}-{recordId}} so multiple buckets can share one ocfl root. */
    private String objectId(String id) {
        return objectIdPrefix() + id;
    }

    private String objectIdPrefix() {
        return config.getBucketId() + "-";
    }

    private Optional<MediaSlot> findUrlSlot() {
        return schema.getMedia().stream().filter(m -> m.getKind() == MediaKind.URL_FILE).findFirst();
    }

    private Optional<RecordIntegrity> readIntegrityFromHead(String id) {
        return readRecord(id).flatMap(tree -> {
            JsonNode integrity = tree.get("integrity");
            if (integrity == null || integrity.isNull()) {
                return Optional.empty();
            }
            return Optional.of(Json.mapper().convertValue(integrity, RecordIntegrity.class));
        });
    }

    private int headVersionNum(String id) {
        ObjectDetails details = repository.describeObject(objectId(id));
        return Math.toIntExact(details.getHeadVersionNum().getVersionNum());
    }

    private static ObjectNode objectChild(ObjectNode parent, String field) {
        JsonNode existing = parent.get(field);
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        return parent.putObject(field);
    }
}
