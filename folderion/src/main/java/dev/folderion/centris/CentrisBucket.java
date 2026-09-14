package dev.folderion.centris;

import dev.folderion.core.Bucket;
import dev.folderion.core.BucketConfig;
import dev.folderion.core.BucketType;
import dev.folderion.core.MediaSlot;
import dev.folderion.core.RecordLayoutSchema;

/**
 * Centris listing bucket contract ({@code centris.listing} v1.0) on OCFL.
 *
 * <p>Use {@link #INSTANCE} (or the static helpers) to {@link #init(Path)} / {@link #open(Path)} a
 * shared {@link Bucket}.
 */
public final class CentrisBucket implements BucketType {

    public static final CentrisBucket INSTANCE = new CentrisBucket();

    public static final String BUCKET_ID = "centris";
    public static final String RECORD_TYPE = "centris.listing";
    public static final String LAYOUT_VERSION = "1.0";
    public static final String IMAGES_SLOT = "images";

    private CentrisBucket() {
    }

    @Override
    public String bucketId() {
        return BUCKET_ID;
    }

    @Override
    public BucketConfig config() {
        BucketConfig config = new BucketConfig(
                BUCKET_ID,
                RECORD_TYPE,
                LAYOUT_VERSION,
                "schemas/centris.listing.v1.json"
        );
        config.setIdPattern("^[0-9]+$");
        return config;
    }

    @Override
    public RecordLayoutSchema schema() {
        return RecordLayoutSchema.builder()
                .recordType(RECORD_TYPE)
                .layoutVersion(LAYOUT_VERSION)
                .path("record", "record.json")
                .path("source_url", "original.url")
                .media(MediaSlot.imageSet(IMAGES_SLOT, "media/images", "media/images/manifest.json"))
                .media(MediaSlot.urlFile("source_url", "original.url"))
                .fingerprintFields(
                        "title", "address", "price", "features", "financial",
                        "description", "brokers", "open_houses")
                .build();
    }
}
