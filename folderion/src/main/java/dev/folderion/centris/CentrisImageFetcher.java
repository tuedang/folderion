package dev.folderion.centris;

import dev.folderion.core.MediaBlob;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Downloads Centris image URLs into {@link MediaBlob}s ready for {@link CentrisWriter}.
 * Does not write to the bucket — call this before {@code writer.write(...)}.
 */
public final class CentrisImageFetcher {

    private final HttpClient httpClient;
    private final Duration timeout;

    public CentrisImageFetcher() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    public CentrisImageFetcher(HttpClient httpClient) {
        this(httpClient, Duration.ofSeconds(60));
    }

    public CentrisImageFetcher(HttpClient httpClient, Duration timeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    /**
     * Download each ref; fails fast on the first HTTP/network error (no partial bucket write).
     */
    public List<MediaBlob> download(List<CentrisImageRef> refs) {
        Objects.requireNonNull(refs, "refs");
        List<MediaBlob> blobs = new ArrayList<>(refs.size());
        for (CentrisImageRef ref : refs) {
            blobs.add(downloadOne(ref));
        }
        return List.copyOf(blobs);
    }

    public MediaBlob downloadOne(CentrisImageRef ref) {
        Objects.requireNonNull(ref, "ref");
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(ref.sourceUrl()))
                    .timeout(timeout)
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new CentrisFetchException("Invalid image URL: " + ref.sourceUrl(), e);
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                throw new CentrisFetchException(
                        "HTTP " + status + " downloading image " + ref.fileName() + " from " + ref.sourceUrl());
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw new CentrisFetchException(
                        "Empty body for image " + ref.fileName() + " from " + ref.sourceUrl());
            }
            return MediaBlob.image(ref.fileName(), body, ref.sourceUrl(), ref.order());
        } catch (CentrisFetchException e) {
            throw e;
        } catch (IOException e) {
            throw new CentrisFetchException(
                    "I/O error downloading image " + ref.fileName() + " from " + ref.sourceUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CentrisFetchException(
                    "Interrupted downloading image " + ref.fileName() + " from " + ref.sourceUrl(), e);
        }
    }
}
