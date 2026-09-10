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

    public CommitResult(Status status, Path recordDir, String contentFingerprint, String mediaFingerprint) {
        this.status = status;
        this.recordDir = recordDir;
        this.contentFingerprint = contentFingerprint;
        this.mediaFingerprint = mediaFingerprint;
    }

    public Status status() {
        return status;
    }

    public Path recordDir() {
        return recordDir;
    }

    public String contentFingerprint() {
        return contentFingerprint;
    }

    public String mediaFingerprint() {
        return mediaFingerprint;
    }

    public boolean written() {
        return status == Status.CREATED || status == Status.UPDATED;
    }
}
