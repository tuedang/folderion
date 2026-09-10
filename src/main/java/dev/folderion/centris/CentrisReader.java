package dev.folderion.centris;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.folderion.core.Bucket;
import dev.folderion.core.FolderionException;
import dev.folderion.core.ImageManifest;
import dev.folderion.core.MediaSlot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads Centris listing records from an OCFL-backed bucket.
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

    /** Record ids present in the bucket (sorted). */
    public List<String> listIds() {
        return bucket.listRecordIds().toList();
    }

    /** HEAD listing for every record id ({@link #listIds()} order). */
    public List<CentrisListing> listAll() {
        List<CentrisListing> listings = new ArrayList<>();
        for (String id : listIds()) {
            listings.add(read(id).orElseThrow(() -> new FolderionException("Missing listing for id: " + id)));
        }
        return listings;
    }

    /**
     * Price at every OCFL version, oldest → newest ({@code v1}…{@code vN}).
     */
    public List<PriceHistoryEntry> priceHistory(String id) {
        List<PriceHistoryEntry> entries = new ArrayList<>();
        for (int version : bucket.listVersions(id)) {
            bucket.readVersionRecord(id, version).ifPresent(node -> {
                CentrisListing.Price price = MAPPER.convertValue(node.get("price"), CentrisListing.Price.class);
                if (price != null) {
                    entries.add(new PriceHistoryEntry("v" + version, price));
                }
            });
        }
        return List.copyOf(entries);
    }

    public record PriceHistoryEntry(String versionId, CentrisListing.Price price) {
    }

    public Optional<String> readme(String id) {
        return bucket.readLogicalText(id, bucket.schema().path("readme")).map(String::trim);
    }

    public Optional<String> sourceUrl(String id) {
        return bucket.readLogicalText(id, bucket.schema().path("source_url")).map(String::trim);
    }

    public Optional<ImageManifest> imageManifest(String id) {
        MediaSlot slot = bucket.schema().requireMedia(CentrisBucket.IMAGES_SLOT);
        if (slot.getManifest() == null) {
            return Optional.empty();
        }
        return bucket.readLogicalJson(id, slot.getManifest())
                .map(node -> MAPPER.convertValue(node, ImageManifest.class));
    }

    /**
     * Paths to HEAD image files under {@code {id}/vN/content/media/images/}.
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
