package dev.folderion.crawler.centris;

import com.fasterxml.jackson.databind.JsonNode;
import dev.folderion.crawler.crawl4ai.Crawl4AiClient;
import dev.folderion.crawler.crawl4ai.Crawl4AiException;
import dev.folderion.crawler.crawl4ai.CrawlRunOptions;
import dev.folderion.crawler.crawl4ai.ExtractionSchemaLoader;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers Centris listing URLs from a search-results page via Crawl4AI + JSON schema.
 */
public final class CentrisSearchCrawler {

    public static final String DEFAULT_SCHEMA = "schemas/centris-search.extraction.json";
    private static final Pattern ID_IN_URL = Pattern.compile("/(\\d{5,})/?$");

    private final Crawl4AiClient client;
    private final ExtractionSchemaLoader schemas;
    private final JsonNode searchSchema;
    private final CrawlRunOptions options;

    public CentrisSearchCrawler(Crawl4AiClient client) {
        this(client, new ExtractionSchemaLoader(), DEFAULT_SCHEMA, CrawlRunOptions.builder()
                .waitFor("css:div.property-thumbnail-item")
                .delayBeforeReturnHtml(2.0)
                .build());
    }

    public CentrisSearchCrawler(
            Crawl4AiClient client,
            ExtractionSchemaLoader schemas,
            String schemaClasspath,
            CrawlRunOptions options) {
        this.client = Objects.requireNonNull(client, "client");
        this.schemas = Objects.requireNonNull(schemas, "schemas");
        this.searchSchema = this.schemas.loadClasspath(schemaClasspath);
        this.options = Objects.requireNonNull(options, "options");
    }

    public List<CentrisSearchHit> crawl(String searchUrl) {
        Objects.requireNonNull(searchUrl, "searchUrl");
        JsonNode request = schemas.buildCrawlRequest(searchUrl, searchSchema, options);
        Crawl4AiClient.CrawlResponse response = client.crawl(request);
        Crawl4AiClient.Result result = response.firstResult();
        if (!result.success()) {
            throw new Crawl4AiException("Search crawl failed: " + result.errorMessage());
        }

        Map<String, CentrisSearchHit> byId = new LinkedHashMap<>();
        for (JsonNode row : result.extracted()) {
            CentrisSearchHit hit = toHit(row);
            if (hit != null && hit.id() != null && !hit.id().isBlank()) {
                byId.putIfAbsent(hit.id(), hit);
            }
        }
        if (byId.isEmpty()) {
            throw new Crawl4AiException("No listing hits extracted from search page: " + searchUrl);
        }
        return List.copyOf(byId.values());
    }

    private static CentrisSearchHit toHit(JsonNode row) {
        String id = text(row, "id");
        String url = text(row, "url");
        String title = text(row, "title");
        if ((id == null || id.isBlank()) && url != null) {
            Matcher matcher = ID_IN_URL.matcher(url);
            if (matcher.find()) {
                id = matcher.group(1);
            }
        }
        if (url == null || url.isBlank()) {
            return null;
        }
        url = absolutize(url);
        if (id == null || id.isBlank()) {
            Matcher matcher = ID_IN_URL.matcher(url);
            if (matcher.find()) {
                id = matcher.group(1);
            }
        }
        return new CentrisSearchHit(id, url, title);
    }

    private static String absolutize(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/")) {
            return URI.create("https://www.centris.ca").resolve(url).toString();
        }
        return "https://www.centris.ca/" + url;
    }

    private static String text(JsonNode row, String field) {
        JsonNode node = row.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value.trim();
    }
}
