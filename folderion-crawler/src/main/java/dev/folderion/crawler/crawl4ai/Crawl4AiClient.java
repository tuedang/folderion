package dev.folderion.crawler.crawl4ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thin HTTP client for Crawl4AI {@code POST /crawl}.
 */
public final class Crawl4AiClient {

    private final Crawl4AiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public Crawl4AiClient(Crawl4AiConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(config.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), new ObjectMapper());
    }

    public Crawl4AiClient(Crawl4AiConfig config, HttpClient httpClient, ObjectMapper mapper) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public CrawlResponse crawl(JsonNode requestBody) {
        Objects.requireNonNull(requestBody, "requestBody");
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(requestBody);
        } catch (IOException e) {
            throw new Crawl4AiException("Failed to serialize crawl request", e);
        }

        HttpRequest request = HttpRequest.newBuilder(config.crawlEndpoint())
                .timeout(config.timeout())
                .header("Authorization", "Bearer " + config.bearerToken())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new Crawl4AiException(
                        "Crawl4AI HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            JsonNode root = mapper.readTree(response.body());
            return CrawlResponse.parse(root, mapper);
        } catch (Crawl4AiException e) {
            throw e;
        } catch (IOException e) {
            throw new Crawl4AiException("I/O error calling Crawl4AI at " + config.crawlEndpoint(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Crawl4AiException("Interrupted calling Crawl4AI at " + config.crawlEndpoint(), e);
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 500 ? body : body.substring(0, 500) + "...";
    }

    /**
     * Parsed Crawl4AI /crawl response focused on extracted JSON rows.
     */
    public record CrawlResponse(boolean success, List<Result> results, String rawError) {

        public static CrawlResponse parse(JsonNode root, ObjectMapper mapper) throws IOException {
            boolean success = root.path("success").asBoolean(true);
            String error = textOrNull(root.get("error"));
            if (error == null) {
                error = textOrNull(root.get("error_message"));
            }

            List<Result> results = new ArrayList<>();
            JsonNode resultsNode = root.get("results");
            if (resultsNode == null && root.has("result")) {
                JsonNode nested = root.get("result");
                resultsNode = nested.get("results");
                if (nested.has("success")) {
                    success = nested.path("success").asBoolean(success);
                }
            }
            if (resultsNode != null && resultsNode.isArray()) {
                for (JsonNode item : resultsNode) {
                    results.add(Result.parse(item, mapper));
                }
            }
            return new CrawlResponse(success, List.copyOf(results), error);
        }

        public Result firstResult() {
            if (results.isEmpty()) {
                throw new Crawl4AiException("Crawl4AI returned no results"
                        + (rawError != null ? ": " + rawError : ""));
            }
            return results.getFirst();
        }
    }

    public record Result(String url, boolean success, String errorMessage, List<JsonNode> extracted) {

        static Result parse(JsonNode item, ObjectMapper mapper) throws IOException {
            String url = textOrNull(item.get("url"));
            boolean success = item.path("success").asBoolean(true);
            String error = textOrNull(item.get("error_message"));
            if (error == null) {
                error = textOrNull(item.get("error"));
            }
            List<JsonNode> extracted = parseExtracted(item.get("extracted_content"), mapper);
            return new Result(url, success, error, extracted);
        }

        private static List<JsonNode> parseExtracted(JsonNode node, ObjectMapper mapper) throws IOException {
            if (node == null || node.isNull()) {
                return List.of();
            }
            if (node.isArray()) {
                List<JsonNode> rows = new ArrayList<>();
                node.forEach(rows::add);
                return List.copyOf(rows);
            }
            if (node.isObject()) {
                return List.of(node);
            }
            if (node.isTextual()) {
                String text = node.asText().trim();
                if (text.isEmpty()) {
                    return List.of();
                }
                JsonNode parsed = mapper.readTree(text);
                if (parsed.isArray()) {
                    List<JsonNode> rows = new ArrayList<>();
                    parsed.forEach(rows::add);
                    return List.copyOf(rows);
                }
                return List.of(parsed);
            }
            return List.of();
        }

        public JsonNode firstExtractedOrThrow() {
            if (!success) {
                throw new Crawl4AiException("Crawl failed for " + url + ": " + errorMessage);
            }
            if (extracted.isEmpty()) {
                throw new Crawl4AiException("No extracted_content for " + url);
            }
            return extracted.getFirst();
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
