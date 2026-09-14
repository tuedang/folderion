package dev.folderion.crawler.crawl4ai;

/**
 * Tunables for a single Crawl4AI {@code CrawlerRunConfig}.
 */
public final class CrawlRunOptions {

    private final boolean stream;
    private final String cacheMode;
    private final String waitFor;
    private final Integer waitForTimeoutMs;
    private final Double delayBeforeReturnHtml;
    private final String cssSelector;

    private CrawlRunOptions(
            boolean stream,
            String cacheMode,
            String waitFor,
            Integer waitForTimeoutMs,
            Double delayBeforeReturnHtml,
            String cssSelector) {
        this.stream = stream;
        this.cacheMode = cacheMode;
        this.waitFor = waitFor;
        this.waitForTimeoutMs = waitForTimeoutMs;
        this.delayBeforeReturnHtml = delayBeforeReturnHtml;
        this.cssSelector = cssSelector;
    }

    public static CrawlRunOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean stream() {
        return stream;
    }

    public String cacheMode() {
        return cacheMode;
    }

    public String waitFor() {
        return waitFor;
    }

    public Integer waitForTimeoutMs() {
        return waitForTimeoutMs;
    }

    public Double delayBeforeReturnHtml() {
        return delayBeforeReturnHtml;
    }

    public String cssSelector() {
        return cssSelector;
    }

    public static final class Builder {
        private boolean stream = false;
        /** Crawl4AI CacheMode — always prefer enabled to avoid re-crawling. */
        private String cacheMode = "enabled";
        private String waitFor = "css:div.region-content";
        private Integer waitForTimeoutMs;
        private Double delayBeforeReturnHtml = 2.0;
        private String cssSelector;

        public Builder stream(boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder cacheMode(String cacheMode) {
            this.cacheMode = cacheMode;
            return this;
        }

        public Builder waitFor(String waitFor) {
            this.waitFor = waitFor;
            return this;
        }

        public Builder waitForTimeoutMs(Integer waitForTimeoutMs) {
            this.waitForTimeoutMs = waitForTimeoutMs;
            return this;
        }

        public Builder delayBeforeReturnHtml(Double delayBeforeReturnHtml) {
            this.delayBeforeReturnHtml = delayBeforeReturnHtml;
            return this;
        }

        public Builder cssSelector(String cssSelector) {
            this.cssSelector = cssSelector;
            return this;
        }

        public CrawlRunOptions build() {
            return new CrawlRunOptions(
                    stream, cacheMode, waitFor, waitForTimeoutMs, delayBeforeReturnHtml, cssSelector);
        }
    }
}
