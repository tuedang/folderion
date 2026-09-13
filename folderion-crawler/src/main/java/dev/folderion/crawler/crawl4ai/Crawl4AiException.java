package dev.folderion.crawler.crawl4ai;

/**
 * Runtime failure talking to Crawl4AI or parsing its response.
 */
public final class Crawl4AiException extends RuntimeException {

    public Crawl4AiException(String message) {
        super(message);
    }

    public Crawl4AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
