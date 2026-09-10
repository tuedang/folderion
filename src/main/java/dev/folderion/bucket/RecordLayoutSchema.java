package dev.folderion.bucket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes how records under a bucket are laid out on disk.
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
    /**
     * Fields that trigger lean history archive when they change (subset of business payload).
     * Empty = no history. Examples: {@code price}, {@code open_houses}.
     */
    private List<String> historyFields = new ArrayList<>();
    /**
     * Max nested lean versions under the record folder ({@code {id}/{id}_v1} … {@code {id}_vN}).
     * {@code 0} disables history. {@code _v1} is the newest archive; higher N is older.
     * Excess versions are dropped on rotate.
     */
    private int maxHistoryVersions = 0;

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

    public List<String> getHistoryFields() {
        return historyFields;
    }

    public void setHistoryFields(List<String> historyFields) {
        this.historyFields = historyFields != null ? new ArrayList<>(historyFields) : new ArrayList<>();
    }

    public int getMaxHistoryVersions() {
        return maxHistoryVersions;
    }

    public void setMaxHistoryVersions(int maxHistoryVersions) {
        this.maxHistoryVersions = maxHistoryVersions;
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
        if (maxHistoryVersions < 0) {
            throw new FolderionException("maxHistoryVersions must be >= 0");
        }
        if (maxHistoryVersions > 0 && (historyFields == null || historyFields.isEmpty())) {
            throw new FolderionException("maxHistoryVersions > 0 requires historyFields");
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

        public Builder historyFields(String... fields) {
            schema.historyFields.addAll(List.of(fields));
            return this;
        }

        public Builder maxHistoryVersions(int maxHistoryVersions) {
            schema.maxHistoryVersions = maxHistoryVersions;
            return this;
        }

        public RecordLayoutSchema build() {
            schema.validate();
            return schema;
        }
    }
}
