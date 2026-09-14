package dev.folderion.crawler.centris;

import com.fasterxml.jackson.databind.JsonNode;
import dev.folderion.centris.CentrisListing;
import dev.folderion.crawler.crawl4ai.Crawl4AiClient;
import dev.folderion.crawler.crawl4ai.CrawlRunOptions;
import dev.folderion.crawler.crawl4ai.ExtractionSchemaLoader;

import java.util.Objects;

/**
 * Crawls a single Centris listing detail page with a classpath JSON extraction schema.
 */
public final class CentrisListingCrawler {

    public static final String DEFAULT_SCHEMA = "schemas/centris-listing.extraction.json";

    private final Crawl4AiClient client;
    private final ExtractionSchemaLoader schemas;
    private final JsonNode listingSchema;
    private final CrawlRunOptions options;
    private final CentrisListingMapper mapper;

    public CentrisListingCrawler(Crawl4AiClient client) {
        this(client, new ExtractionSchemaLoader(), DEFAULT_SCHEMA, CrawlRunOptions.builder()
                .cacheMode("enabled")
                .waitFor("css:div.region-content")
                .delayBeforeReturnHtml(2.0)
                .build(), new CentrisListingMapper());
    }

    public CentrisListingCrawler(
            Crawl4AiClient client,
            ExtractionSchemaLoader schemas,
            String schemaClasspath,
            CrawlRunOptions options,
            CentrisListingMapper mapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.schemas = Objects.requireNonNull(schemas, "schemas");
        this.listingSchema = this.schemas.loadClasspath(schemaClasspath);
        this.options = Objects.requireNonNull(options, "options");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public CentrisListing crawl(String listingUrl) {
        Objects.requireNonNull(listingUrl, "listingUrl");
        JsonNode request = schemas.buildCrawlRequest(listingUrl, listingSchema, options);
        Crawl4AiClient.Result result = client.crawl(request).firstResult();
        JsonNode extracted = result.firstExtractedOrThrow();
        return mapper.map(extracted, listingUrl);
    }
}
