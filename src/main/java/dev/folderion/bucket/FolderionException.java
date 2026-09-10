package dev.folderion.bucket;

public final class FolderionException extends RuntimeException {

    public FolderionException(String message) {
        super(message);
    }

    public FolderionException(String message, Throwable cause) {
        super(message, cause);
    }
}
