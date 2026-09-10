package dev.folderion.centris;

/**
 * Failed to download a Centris image URL.
 */
public final class CentrisFetchException extends RuntimeException {

    public CentrisFetchException(String message) {
        super(message);
    }

    public CentrisFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
