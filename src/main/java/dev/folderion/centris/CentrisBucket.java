package dev.folderion.centris;

import dev.folderion.bucket.Bucket;
import dev.folderion.bucket.BucketConfig;
import dev.folderion.bucket.Folderion;
import dev.folderion.bucket.MediaSlot;
import dev.folderion.bucket.RecordLayoutSchema;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Factory for the Centris listing bucket contract ({@code centris.listing} v1.0).
 */
public final class CentrisBucket {

    public static final String BUCKET_ID = "centris";
    public static final String RECORD_TYPE = "centris.listing";
    public static final String LAYOUT_VERSION = "1.0";
    public static final String IMAGES_SLOT = "images";

    private CentrisBucket() {
    }

    public static Bucket init(Path bucketRoot) {
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        return Folderion.create().init(bucketRoot, config(), schema());
    }

    public static Bucket open(Path bucketRoot) {
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        return Folderion.create().open(bucketRoot);
    }

    public static BucketConfig config() {
        BucketConfig config = new BucketConfig(
                BUCKET_ID,
                RECORD_TYPE,
                LAYOUT_VERSION,
                "schemas/centris.listing.v1.json"
        );
        config.setIdPattern("^[0-9]+$");
        return config;
    }

    public static RecordLayoutSchema schema() {
        return RecordLayoutSchema.builder()
                .recordType(RECORD_TYPE)
                .layoutVersion(LAYOUT_VERSION)
                .path("record", "record.json")
                .path("readme", "README.md")
                .path("source_url", "source/original.url")
                .media(MediaSlot.imageSet(IMAGES_SLOT, "media/images", "media/images/manifest.json"))
                .media(MediaSlot.urlFile("source_url", "source/original.url"))
                .fingerprintFields(
                        "title", "address", "price", "features", "financial",
                        "description", "brokers", "open_houses")
                // Lean history: nested {id}/{id}_v1.._vN (HEAD = parent folder); keep last 3.
                .historyFields("price")
                .maxHistoryVersions(3)
                .build();
    }
}
