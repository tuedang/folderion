package dev.folderion.centris;

import com.sun.net.httpserver.HttpServer;
import dev.folderion.core.MediaBlob;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fetcher tests use a loopback {@link HttpServer} — no external network.
 */
class CentrisImageFetcherTest {

    @Test
    void downloadMapsRefsToMediaBlobs() throws Exception {
        byte[] body = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);
        HttpServer server = startServer(exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            List<CentrisImageRef> refs = List.of(
                    new CentrisImageRef("01.jpg", base + "/01.jpg", 1),
                    new CentrisImageRef("02.jpg", base + "/02.jpg", 2)
            );

            List<MediaBlob> blobs = new CentrisImageFetcher(HttpClient.newHttpClient()).download(refs);

            assertEquals(2, blobs.size());
            assertEquals("01.jpg", blobs.get(0).fileName());
            assertEquals(base + "/01.jpg", blobs.get(0).sourceUrl());
            assertEquals(1, blobs.get(0).order());
            assertEquals("fake-jpeg-bytes", new String(blobs.get(0).bytes(), StandardCharsets.UTF_8));
            assertEquals("02.jpg", blobs.get(1).fileName());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void non2xxThrowsCentrisFetchException() throws Exception {
        HttpServer server = startServer(exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/missing.jpg";
            CentrisImageRef ref = new CentrisImageRef("01.jpg", url, 1);

            CentrisFetchException ex = assertThrows(
                    CentrisFetchException.class,
                    () -> new CentrisImageFetcher(HttpClient.newHttpClient()).downloadOne(ref));
            assertTrue(ex.getMessage().contains("404"));
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", handler);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        return server;
    }
}
