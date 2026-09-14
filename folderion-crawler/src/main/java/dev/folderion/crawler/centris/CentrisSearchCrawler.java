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
    public static final int DEFAULT_MAX_PAGES = 200;

    private static final Pattern ID_IN_URL = Pattern.compile("/(\\d{5,})/?$");

    private final Crawl4AiClient client;
    private final ExtractionSchemaLoader schemas;
    private final JsonNode searchSchema;
    private final CrawlRunOptions options;
    private final int maxPages;

    public CentrisSearchCrawler(Crawl4AiClient client) {
        this(client, new ExtractionSchemaLoader(), DEFAULT_SCHEMA, CrawlRunOptions.builder()
                .cacheMode("enabled")
                .waitFor("css:#divMainResult")
                .waitForTimeoutMs(20_000)
                .delayBeforeReturnHtml(5.0)
                .build(), DEFAULT_MAX_PAGES);
    }

    public CentrisSearchCrawler(
            Crawl4AiClient client,
            ExtractionSchemaLoader schemas,
            String schemaClasspath,
            CrawlRunOptions options) {
        this(client, schemas, schemaClasspath, options, DEFAULT_MAX_PAGES);
    }

    public CentrisSearchCrawler(
            Crawl4AiClient client,
            ExtractionSchemaLoader schemas,
            String schemaClasspath,
            CrawlRunOptions options,
            int maxPages) {
        this.client = Objects.requireNonNull(client, "client");
        this.schemas = Objects.requireNonNull(schemas, "schemas");
        this.searchSchema = this.schemas.loadClasspath(schemaClasspath);
        this.options = Objects.requireNonNull(options, "options");
        if (maxPages < 1) {
            throw new IllegalArgumentException("maxPages must be >= 1");
        }
        this.maxPages = maxPages;
    }

    /**
     * Crawl a single search URL as-is (no pagination).
     */
    public List<CentrisSearchHit> crawl(String searchUrl) {
        return crawl(searchUrl, false);
    }

    /**
     * @param paginate when {@code true}, walk {@code page=1..N} until a page returns no new hits
     */
    public List<CentrisSearchHit> crawl(String searchUrl, boolean paginate) {
        Objects.requireNonNull(searchUrl, "searchUrl");
        if (!paginate) {
            return crawlSinglePage(searchUrl);
        }

        int pageSize = CentrisSearchUrls.pageSize(searchUrl);
        Map<String, CentrisSearchHit> byId = new LinkedHashMap<>();

        for (int page = 1; page <= maxPages; page++) {
            String pageUrl = CentrisSearchUrls.withPage(searchUrl, page);
            // Soft-fail wait timeouts only after page 1 — page 1 must actually extract hits.
            List<CentrisSearchHit> pageHits = crawlSinglePageAllowEmpty(pageUrl, page > 1);
            System.out.printf("SEARCH page=%d hits=%d url=%s%n", page, pageHits.size(), pageUrl);

            int before = byId.size();
            for (CentrisSearchHit hit : pageHits) {
                byId.putIfAbsent(hit.id(), hit);
            }
            int added = byId.size() - before;

            if (pageHits.isEmpty() || added == 0) {
                break;
            }
            // Last page typically has fewer cards than pageSize.
            if (pageHits.size() < pageSize) {
                break;
            }
        }

        if (byId.isEmpty()) {
            throw new Crawl4AiException("No listing hits extracted across search pages: " + searchUrl);
        }
        return List.copyOf(byId.values());
    }

    private List<CentrisSearchHit> crawlSinglePage(String searchUrl) {
        List<CentrisSearchHit> hits = crawlSinglePageAllowEmpty(searchUrl, false);
        if (hits.isEmpty()) {
            throw new Crawl4AiException("No listing hits extracted from search page: " + searchUrl);
        }
        return hits;
    }

    private List<CentrisSearchHit> crawlSinglePageAllowEmpty(String searchUrl, boolean softFailEmptyPage) {
        JsonNode request = schemas.buildCrawlRequest(searchUrl, searchSchema, options);
        Crawl4AiClient.CrawlResponse response = client.crawl(request);
        Crawl4AiClient.Result result = response.firstResult();
        if (!result.success()) {
            if (softFailEmptyPage && isNoResultsPage(result.errorMessage())) {
                System.out.printf("SEARCH empty/timeout treated as end: %s%n", summarize(result.errorMessage()));
                return List.of();
            }
            throw new Crawl4AiException("Search crawl failed: " + result.errorMessage());
        }

        Map<String, CentrisSearchHit> byId = new LinkedHashMap<>();
        for (JsonNode row : result.extracted()) {
            CentrisSearchHit hit = toHit(row);
            if (hit != null && hit.id() != null && !hit.id().isBlank()) {
                byId.putIfAbsent(hit.id(), hit);
            }
        }
        return List.copyOf(byId.values());
    }

    /** Empty / out-of-range Centris pages never render listing cards; wait timeouts mean "no hits". */
    private static boolean isNoResultsPage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return false;
        }
        String lower = errorMessage.toLowerCase();
        return lower.contains("wait condition failed")
                || lower.contains("timeout")
                || lower.contains("property-thumbnail-item");
    }

    private static String summarize(String errorMessage) {
        if (errorMessage == null) {
            return "";
        }
        String oneLine = errorMessage.replace('\n', ' ').trim();
        return oneLine.length() <= 160 ? oneLine : oneLine.substring(0, 160) + "...";
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
