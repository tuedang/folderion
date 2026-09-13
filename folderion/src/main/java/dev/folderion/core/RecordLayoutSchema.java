package dev.folderion.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes how records under a bucket are laid out as OCFL logical paths.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class RecordLayoutSchema {

    private String recordType;
    private String layoutVersion;
    private Map<String, String> paths = new LinkedHashMap<>();
    private List<MediaSlot> media = new ArrayList<>();
    /** JSON field names included in content fingerprint (top-level or dotted object paths). */
    private List<String> fingerprintFields = new ArrayList<>();

    public RecordLayoutSchema() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getLayoutVersion() {
        return layoutVersion;
    }

    public void setLayoutVersion(String layoutVersion) {
        this.layoutVersion = layoutVersion;
    }

    public Map<String, String> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, String> paths) {
        this.paths = paths != null ? new LinkedHashMap<>(paths) : new LinkedHashMap<>();
    }

    public List<MediaSlot> getMedia() {
        return media;
    }

    public void setMedia(List<MediaSlot> media) {
        this.media = media != null ? new ArrayList<>(media) : new ArrayList<>();
    }

    public List<String> getFingerprintFields() {
        return fingerprintFields;
    }

    public void setFingerprintFields(List<String> fingerprintFields) {
        this.fingerprintFields = fingerprintFields != null ? new ArrayList<>(fingerprintFields) : new ArrayList<>();
    }

    public String path(String key) {
        String value = paths.get(key);
        if (value == null || value.isBlank()) {
            throw new FolderionException("Schema path missing: " + key);
        }
        return value;
    }

    public String pathOrDefault(String key, String defaultPath) {
        String value = paths.get(key);
        return value == null || value.isBlank() ? defaultPath : value;
    }

    public MediaSlot requireMedia(String key) {
        return media.stream()
                .filter(m -> key.equals(m.getKey()))
                .findFirst()
                .orElseThrow(() -> new FolderionException("Unknown media slot: " + key));
    }

    public void validate() {
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(layoutVersion, "layoutVersion");
        if (!paths.containsKey("record")) {
            throw new FolderionException("paths.record is required");
        }
        for (MediaSlot slot : media) {
            slot.validate();
        }
    }

    public static final class Builder {
        private final RecordLayoutSchema schema = new RecordLayoutSchema();

        public Builder recordType(String recordType) {
            schema.recordType = recordType;
            return this;
        }

        public Builder layoutVersion(String layoutVersion) {
            schema.layoutVersion = layoutVersion;
            return this;
        }

        public Builder path(String key, String relativePath) {
            schema.paths.put(key, relativePath);
            return this;
        }

        public Builder media(MediaSlot slot) {
            schema.media.add(slot);
            return this;
        }

        public Builder fingerprintFields(String... fields) {
            schema.fingerprintFields.addAll(List.of(fields));
            return this;
        }

        public RecordLayoutSchema build() {
            schema.validate();
            return schema;
        }
    }
}
