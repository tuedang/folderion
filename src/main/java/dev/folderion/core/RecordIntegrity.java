package dev.folderion.core;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RecordIntegrity {

    private String contentSha256;
    private String mediaSha256;
    private String algo = "sha256:v1";

    public RecordIntegrity() {
    }

    public RecordIntegrity(String contentSha256, String mediaSha256) {
        this.contentSha256 = Objects.requireNonNull(contentSha256);
        this.mediaSha256 = mediaSha256;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public String getMediaSha256() {
        return mediaSha256;
    }

    public void setMediaSha256(String mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    public String getAlgo() {
        return algo;
    }

    public void setAlgo(String algo) {
        this.algo = algo;
    }
}
