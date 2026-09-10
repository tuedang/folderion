package dev.folderion.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Bucket-level contract: one bucket = one record type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BucketConfig {

    private String bucketId;
    private String recordType;
    private String layoutVersion;
    private String schema;
    private String idPattern;
    private String recordsDir = "records";

    public BucketConfig() {
    }

    public BucketConfig(String bucketId, String recordType, String layoutVersion, String schema) {
        this.bucketId = Objects.requireNonNull(bucketId, "bucketId");
        this.recordType = Objects.requireNonNull(recordType, "recordType");
        this.layoutVersion = Objects.requireNonNull(layoutVersion, "layoutVersion");
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
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

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getIdPattern() {
        return idPattern;
    }

    public void setIdPattern(String idPattern) {
        this.idPattern = idPattern;
    }

    public String getRecordsDir() {
        return recordsDir == null || recordsDir.isBlank() ? "records" : recordsDir;
    }

    public void setRecordsDir(String recordsDir) {
        this.recordsDir = recordsDir;
    }
}
