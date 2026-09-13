package dev.folderion.crawler.crawl4ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractionSchemaLoaderTest {

    private final ExtractionSchemaLoader loader = new ExtractionSchemaLoader();

    @Test
    void loadsClasspathSchemasAndWrapsDict() {
        JsonNode search = loader.loadClasspath("schemas/centris-search.extraction.json");
        JsonNode listing = loader.loadClasspath("schemas/centris-listing.extraction.json");

        assertEquals("CentrisSearchResults", search.path("name").asText());
        assertEquals("CentrisListing", listing.path("name").asText());

        ObjectNode strategy = loader.wrapAsExtractionStrategy(listing);
        assertEquals("JsonCssExtractionStrategy", strategy.path("type").asText());
        assertEquals("dict", strategy.path("params").path("schema").path("type").asText());
        assertTrue(strategy.path("params").path("schema").path("value").path("fields").isArray());
    }

    @Test
    void buildsCrawlRequestWithBearerReadyBody() {
        JsonNode schema = loader.loadClasspath("schemas/centris-listing.extraction.json");
        ObjectNode request = loader.buildCrawlRequest(
                "https://www.centris.ca/en/houses~for-sale~brossard/17351555",
                schema,
                CrawlRunOptions.defaults());

        assertEquals(1, request.path("urls").size());
        assertEquals("CrawlerRunConfig", request.path("crawler_config").path("type").asText());
        assertEquals(
                "JsonCssExtractionStrategy",
                request.path("crawler_config").path("params").path("extraction_strategy").path("type").asText());
        assertEquals(
                "dict",
                request.path("crawler_config").path("params").path("extraction_strategy")
                        .path("params").path("schema").path("type").asText());
    }
}
