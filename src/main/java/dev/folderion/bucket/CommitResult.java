package dev.folderion.bucket;

import java.nio.file.Path;

public final class CommitResult {

    public enum Status {
        CREATED,
        UPDATED,
        UNCHANGED
    }

    private final Status status;
    private final Path recordDir;
    private final String contentFingerprint;
    private final String mediaFingerprint;
    private final int version;

    public CommitResult(Status status, Path recordDir, String contentFingerprint, String mediaFingerprint) {
        this(status, recordDir, contentFingerprint, mediaFingerprint, 0);
    }

    public CommitResult(
            Status status,
            Path recordDir,
            String contentFingerprint,
            String mediaFingerprint,
            int version) {
        this.status = status;
        this.recordDir = recordDir;
        this.contentFingerprint = contentFingerprint;
        this.mediaFingerprint = mediaFingerprint;
        this.version = version;
    }

    public Status status() {
        return status;
    }

    /** HEAD logical files under {@code buckets/centris/centris-{id}/vN/content/}. */
    public Path recordDir() {
        return recordDir;
    }

    public String contentFingerprint() {
        return contentFingerprint;
    }

    public String mediaFingerprint() {
        return mediaFingerprint;
    }

    /** OCFL version number ({@code 1} = v1), or {@code 0} if unknown/unchanged without read. */
    public int version() {
        return version;
    }

    public boolean written() {
        return status == Status.CREATED || status == Status.UPDATED;
    }
}
