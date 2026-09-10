package dev.folderion.bucket;

import java.util.Objects;

/**
 * A blob to persist under a media slot (image, audio, etc.).
 */
public final class MediaBlob {

    private final String fileName;
    private final byte[] bytes;
    private final String sourceUrl;
    private final Integer order;
    private final String contentType;

    private MediaBlob(String fileName, byte[] bytes, String sourceUrl, Integer order, String contentType) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.bytes = Objects.requireNonNull(bytes, "bytes");
        this.sourceUrl = sourceUrl;
        this.order = order;
        this.contentType = contentType;
    }

    public static MediaBlob of(String fileName, byte[] bytes) {
        return new MediaBlob(fileName, bytes, null, null, null);
    }

    public static MediaBlob image(String fileName, byte[] bytes, String sourceUrl, int order) {
        return new MediaBlob(fileName, bytes, sourceUrl, order, "image/jpeg");
    }

    public String fileName() {
        return fileName;
    }

    public byte[] bytes() {
        return bytes;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public Integer order() {
        return order;
    }

    public String contentType() {
        return contentType;
    }

    public String sha256() {
        return Fingerprints.sha256Hex(bytes);
    }
}
