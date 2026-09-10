package dev.folderion.centris;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.folderion.bucket.Bucket;
import dev.folderion.bucket.FolderionException;
import dev.folderion.bucket.ImageManifest;
import dev.folderion.bucket.MediaSlot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads Centris listing records from a bucket (typed DTO + local image paths).
 */
public final class CentrisReader {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Bucket bucket;

    public CentrisReader(Bucket bucket) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
    }

    public Optional<CentrisListing> read(String id) {
        Optional<JsonNode> record = bucket.readRecord(id);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        CentrisListing listing = MAPPER.convertValue(record.get(), CentrisListing.class);
        if (listing.getId() == null) {
            listing.setId(id);
        }
        listing.setImageRefs(imageRefsFromManifest(id));
        return Optional.of(listing);
    }

    /**
     * Lean price timeline: archived versions oldest→newest ({@code _vN}…{@code _v1}), then HEAD ({@code current}).
     * {@code _v1} is the newest archive.
     */
    public List<PriceHistoryEntry> priceHistory(String id) {
        List<PriceHistoryEntry> entries = new ArrayList<>();
        List<Integer> versions = bucket.listHistoryVersions(id);
        for (int i = versions.size() - 1; i >= 0; i--) {
            int version = versions.get(i);
            bucket.readHistoryRecord(id, version).ifPresent(node -> {
                CentrisListing.Price price = MAPPER.convertValue(node.get("price"), CentrisListing.Price.class);
                if (price != null) {
                    entries.add(new PriceHistoryEntry("v" + version, price));
                }
            });
        }
        read(id).ifPresent(listing -> {
            if (listing.getPrice() != null) {
                entries.add(new PriceHistoryEntry("current", listing.getPrice()));
            }
        });
        return List.copyOf(entries);
    }

    public record PriceHistoryEntry(String versionId, CentrisListing.Price price) {
    }

    public Optional<String> readme(String id) {
        Path file = bucket.recordDir(id).resolve(bucket.schema().path("readme"));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file));
        } catch (IOException e) {
            throw new FolderionException("Failed to read README: " + file, e);
        }
    }

    public Optional<String> sourceUrl(String id) {
        Path file = bucket.recordDir(id).resolve(bucket.schema().path("source_url"));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(file).trim());
        } catch (IOException e) {
            throw new FolderionException("Failed to read source URL: " + file, e);
        }
    }

    public Optional<ImageManifest> imageManifest(String id) {
        MediaSlot slot = bucket.schema().requireMedia(CentrisBucket.IMAGES_SLOT);
        if (slot.getManifest() == null) {
            return Optional.empty();
        }
        Path file = bucket.recordDir(id).resolve(slot.getManifest());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try (InputStream in = Files.newInputStream(file)) {
            return Optional.of(MAPPER.readValue(in, ImageManifest.class));
        } catch (IOException e) {
            throw new FolderionException("Failed to read image manifest: " + file, e);
        }
    }

    /**
     * Absolute paths to image files under the record's image set directory (ordered by manifest when present).
     */
    public List<Path> imagePaths(String id) {
        MediaSlot slot = bucket.schema().requireMedia(CentrisBucket.IMAGES_SLOT);
        Path dir = bucket.recordDir(id).resolve(slot.getDir());
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        Optional<ImageManifest> manifest = imageManifest(id);
        if (manifest.isPresent() && !manifest.get().getItems().isEmpty()) {
            List<Path> paths = new ArrayList<>();
            for (ImageManifest.Item item : manifest.get().getItems()) {
                Path file = dir.resolve(item.getFile());
                if (Files.isRegularFile(file)) {
                    paths.add(file.toAbsolutePath().normalize());
                }
            }
            return List.copyOf(paths);
        }

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().equals("manifest.json"))
                    .sorted()
                    .map(p -> p.toAbsolutePath().normalize())
                    .toList();
        } catch (IOException e) {
            throw new FolderionException("Failed to list images in " + dir, e);
        }
    }

    private List<CentrisImageRef> imageRefsFromManifest(String id) {
        Optional<ImageManifest> manifest = imageManifest(id);
        if (manifest.isEmpty()) {
            return List.of();
        }
        List<CentrisImageRef> refs = new ArrayList<>();
        for (ImageManifest.Item item : manifest.get().getItems()) {
            int order = item.getOrder() != null ? item.getOrder() : refs.size() + 1;
            String url = item.getSourceUrl() != null ? item.getSourceUrl() : "";
            refs.add(new CentrisImageRef(item.getFile(), url, order));
        }
        return List.copyOf(refs);
    }
}
