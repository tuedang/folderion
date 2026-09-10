package dev.folderion.centris;

import dev.folderion.bucket.Bucket;
import dev.folderion.bucket.CommitResult;
import dev.folderion.bucket.MediaBlob;
import dev.folderion.bucket.WritePlan;

import java.util.List;
import java.util.Objects;

/**
 * Writes a {@link CentrisListing} (+ prepared image bytes) into a Centris bucket via {@link WritePlan}.
 */
public final class CentrisWriter {

    private final Bucket bucket;

    public CentrisWriter(Bucket bucket) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
    }

    /**
     * Persist listing metadata, README, source URL, and optional image blobs.
     *
     * @param listing parsed listing DTO
     * @param images  prepared image bytes (may be empty); typically from {@link CentrisImageFetcher}
     */
    public CommitResult write(CentrisListing listing, List<MediaBlob> images) {
        Objects.requireNonNull(listing, "listing");
        Objects.requireNonNull(listing.getId(), "listing.id");
        List<MediaBlob> imageList = images != null ? List.copyOf(images) : List.of();

        WritePlan.Builder plan = WritePlan.builder()
                .id(listing.getId())
                .record(listing.toRecordPayload())
                .sourceUrl(listing.sourceUrl())
                .readmeMarkdown(CentrisReadme.generate(listing));

        if (!imageList.isEmpty()) {
            plan.images(CentrisBucket.IMAGES_SLOT, imageList);
        }

        return bucket.commit(plan.build());
    }
}
