package dev.folderion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Intent to write one record folder. Built by ingest adapters; persisted by {@link Bucket}.
 */
public final class WritePlan {

    private final String id;
    private final ObjectNode record;
    private final String readmeMarkdown;
    private final String sourceUrl;
    private final Map<String, List<MediaBlob>> mediaBySlot;
    private final Map<String, byte[]> singleBlobsBySlot;

    private WritePlan(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.record = Objects.requireNonNull(builder.record, "record");
        this.readmeMarkdown = builder.readmeMarkdown;
        this.sourceUrl = builder.sourceUrl;
        this.mediaBySlot = Map.copyOf(builder.mediaBySlot);
        this.singleBlobsBySlot = Map.copyOf(builder.singleBlobsBySlot);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String id() {
        return id;
    }

    public ObjectNode record() {
        return record;
    }

    public String readmeMarkdown() {
        return readmeMarkdown;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public Map<String, List<MediaBlob>> mediaBySlot() {
        return mediaBySlot;
    }

    public Map<String, byte[]> singleBlobsBySlot() {
        return singleBlobsBySlot;
    }

    public static final class Builder {
        private String id;
        private ObjectNode record;
        private String readmeMarkdown;
        private String sourceUrl;
        private final Map<String, List<MediaBlob>> mediaBySlot = new LinkedHashMap<>();
        private final Map<String, byte[]> singleBlobsBySlot = new LinkedHashMap<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder record(Object recordObject) {
            JsonNode tree = Json.valueToTree(recordObject);
            if (!tree.isObject()) {
                throw new FolderionException("record must be a JSON object");
            }
            this.record = (ObjectNode) tree;
            return this;
        }

        public Builder record(ObjectNode record) {
            this.record = record;
            return this;
        }

        public Builder readmeMarkdown(String readmeMarkdown) {
            this.readmeMarkdown = readmeMarkdown;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder images(String slotKey, List<MediaBlob> images) {
            this.mediaBySlot.put(slotKey, List.copyOf(images));
            return this;
        }

        public Builder blob(String slotKey, byte[] bytes) {
            this.singleBlobsBySlot.put(slotKey, bytes);
            return this;
        }

        public WritePlan build() {
            if (record != null && id != null && !record.has("id")) {
                record.put("id", id);
            }
            return new WritePlan(this);
        }
    }
}
