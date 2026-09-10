package dev.folderion.centris;

import java.util.Objects;

/**
 * Metadata for one Centris listing image (no bytes — prepare via {@link CentrisImageFetcher} or caller).
 */
public final class CentrisImageRef {

    private final String fileName;
    private final String sourceUrl;
    private final int order;

    public CentrisImageRef(String fileName, String sourceUrl, int order) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.sourceUrl = Objects.requireNonNull(sourceUrl, "sourceUrl");
        this.order = order;
    }

    public String fileName() {
        return fileName;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public int order() {
        return order;
    }
}
