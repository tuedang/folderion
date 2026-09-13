package dev.folderion.crawler.crawl4ai;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for a Crawl4AI Docker / HTTP API endpoint.
 */
public final class Crawl4AiConfig {

    public static final String DEFAULT_BASE_URL = "http://192.168.68.57:11235";
    public static final String DEFAULT_TOKEN = "mytoken";

    private final URI baseUrl;
    private final String bearerToken;
    private final Duration timeout;

    public Crawl4AiConfig(URI baseUrl, String bearerToken, Duration timeout) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.bearerToken = Objects.requireNonNull(bearerToken, "bearerToken");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public static Crawl4AiConfig defaults() {
        return fromEnv();
    }

    /**
     * Reads {@code CRAWL4AI_URL} / {@code CRAWL4AI_TOKEN} when set; otherwise defaults.
     */
    public static Crawl4AiConfig fromEnv() {
        String url = firstNonBlank(System.getenv("CRAWL4AI_URL"), System.getProperty("crawl4ai.url"), DEFAULT_BASE_URL);
        String token = firstNonBlank(System.getenv("CRAWL4AI_TOKEN"), System.getProperty("crawl4ai.token"), DEFAULT_TOKEN);
        return new Crawl4AiConfig(URI.create(trimTrailingSlash(url)), token, Duration.ofMinutes(3));
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public URI crawlEndpoint() {
        return baseUrl.resolve("/crawl");
    }

    public String bearerToken() {
        return bearerToken;
    }

    public Duration timeout() {
        return timeout;
    }

    public Crawl4AiConfig withBaseUrl(String url) {
        return new Crawl4AiConfig(URI.create(trimTrailingSlash(url)), bearerToken, timeout);
    }

    public Crawl4AiConfig withToken(String token) {
        return new Crawl4AiConfig(baseUrl, token, timeout);
    }

    public Crawl4AiConfig withTimeout(Duration timeout) {
        return new Crawl4AiConfig(baseUrl, bearerToken, timeout);
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        throw new IllegalStateException("No non-blank value");
    }
}
