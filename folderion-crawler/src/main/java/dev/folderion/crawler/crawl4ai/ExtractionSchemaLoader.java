package dev.folderion.crawler.crawl4ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Loads a Crawl4AI {@code JsonCssExtractionStrategy} schema from a JSON file (classpath or path).
 */
public final class ExtractionSchemaLoader {

    private final ObjectMapper mapper;

    public ExtractionSchemaLoader() {
        this(new ObjectMapper());
    }

    public ExtractionSchemaLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public JsonNode loadClasspath(String resourcePath) {
        Objects.requireNonNull(resourcePath, "resourcePath");
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized)) {
            if (in == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + normalized);
            }
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read schema: " + normalized, e);
        }
    }

    public JsonNode loadFile(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            return mapper.readTree(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read schema file: " + path, e);
        }
    }

    /**
     * Builds the Crawl4AI Docker API {@code extraction_strategy} node with schema wrapped as
     * {@code {"type":"dict","value":...}} so field {@code "type"} values are not mis-deserialized.
     */
    public ObjectNode wrapAsExtractionStrategy(JsonNode schema) {
        Objects.requireNonNull(schema, "schema");
        ObjectNode dictWrapper = mapper.createObjectNode();
        dictWrapper.put("type", "dict");
        dictWrapper.set("value", schema);

        ObjectNode params = mapper.createObjectNode();
        params.set("schema", dictWrapper);

        ObjectNode strategy = mapper.createObjectNode();
        strategy.put("type", "JsonCssExtractionStrategy");
        strategy.set("params", params);
        return strategy;
    }

    public ObjectNode buildCrawlRequest(String url, JsonNode schema, CrawlRunOptions options) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(schema, "schema");
        CrawlRunOptions opts = options != null ? options : CrawlRunOptions.defaults();

        ArrayNode urls = mapper.createArrayNode();
        urls.add(url);

        ObjectNode params = mapper.createObjectNode();
        params.put("stream", opts.stream());
        params.set("cache_mode", wrapCacheMode(opts.cacheMode()));
        if (opts.waitFor() != null && !opts.waitFor().isBlank()) {
            params.put("wait_for", opts.waitFor());
        }
        if (opts.waitForTimeoutMs() != null) {
            params.put("wait_for_timeout", opts.waitForTimeoutMs());
        }
        if (opts.delayBeforeReturnHtml() != null) {
            params.put("delay_before_return_html", opts.delayBeforeReturnHtml());
        }
        if (opts.cssSelector() != null && !opts.cssSelector().isBlank()) {
            params.put("css_selector", opts.cssSelector());
        }
        params.set("extraction_strategy", wrapAsExtractionStrategy(schema));

        ObjectNode crawlerConfig = mapper.createObjectNode();
        crawlerConfig.put("type", "CrawlerRunConfig");
        crawlerConfig.set("params", params);

        ObjectNode root = mapper.createObjectNode();
        root.set("urls", urls);
        root.set("crawler_config", crawlerConfig);
        return root;
    }

    /** Docker API needs {@code {"type":"CacheMode","params":"enabled"}} — a raw string never becomes the Enum. */
    ObjectNode wrapCacheMode(String cacheMode) {
        String value = cacheMode == null || cacheMode.isBlank() ? "enabled" : cacheMode;
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "CacheMode");
        node.put("params", value);
        return node;
    }
}
