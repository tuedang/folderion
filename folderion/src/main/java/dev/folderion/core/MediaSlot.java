package dev.folderion.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class MediaSlot {

    private String key;
    private MediaKind kind;
    /** Directory for multi-file kinds (e.g. IMAGE_SET). */
    private String dir;
    /** Single file path relative to record root. */
    private String path;
    /** Optional manifest path (IMAGE_SET). */
    private String manifest;

    public MediaSlot() {
    }

    public static MediaSlot imageSet(String key, String dir, String manifest) {
        MediaSlot slot = new MediaSlot();
        slot.key = key;
        slot.kind = MediaKind.IMAGE_SET;
        slot.dir = dir;
        slot.manifest = manifest;
        return slot;
    }

    public static MediaSlot urlFile(String key, String path) {
        MediaSlot slot = new MediaSlot();
        slot.key = key;
        slot.kind = MediaKind.URL_FILE;
        slot.path = path;
        return slot;
    }

    public static MediaSlot markdown(String key, String path) {
        MediaSlot slot = new MediaSlot();
        slot.key = key;
        slot.kind = MediaKind.MARKDOWN;
        slot.path = path;
        return slot;
    }

    public static MediaSlot audioFile(String key, String path) {
        MediaSlot slot = new MediaSlot();
        slot.key = key;
        slot.kind = MediaKind.AUDIO_FILE;
        slot.path = path;
        return slot;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MediaKind getKind() {
        return kind;
    }

    public void setKind(MediaKind kind) {
        this.kind = kind;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public void validate() {
        Objects.requireNonNull(key, "media.key");
        Objects.requireNonNull(kind, "media.kind");
        switch (kind) {
            case IMAGE_SET -> {
                if (dir == null || dir.isBlank()) {
                    throw new FolderionException("IMAGE_SET '" + key + "' requires dir");
                }
            }
            case AUDIO_FILE, VIDEO_FILE, MARKDOWN, JSON_DOC, URL_FILE, BLOB_FILE -> {
                if (path == null || path.isBlank()) {
                    throw new FolderionException(kind + " '" + key + "' requires path");
                }
            }
        }
    }
}
