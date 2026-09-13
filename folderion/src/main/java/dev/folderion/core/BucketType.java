package dev.folderion.core;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Contract for a bucket kind (e.g. Centris listing): config + layout schema.
 *
 * <p>Domain modules implement this and call {@link #init(Path)} / {@link #open(Path)} to obtain a
 * shared {@link Bucket} handle. Example:
 *
 * <pre>{@code
 * try (Bucket bucket = CentrisBucket.INSTANCE.init(Path.of("buckets/centris"))) {
 *     new CentrisWriter(bucket).write(listing, images);
 * }
 * }</pre>
 */
public interface BucketType {

    /** Stable id written into {@code bucket.json} (also prefixes OCFL object ids). */
    String bucketId();

    BucketConfig config();

    RecordLayoutSchema schema();

    /** Create Folderion meta + OCFL storage root at {@code bucketRoot}. */
    default Bucket init(Path bucketRoot) {
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        return Folderion.create().init(bucketRoot, this);
    }

    /** Open an existing bucket directory. */
    default Bucket open(Path bucketRoot) {
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        return Folderion.create().open(bucketRoot);
    }
}
