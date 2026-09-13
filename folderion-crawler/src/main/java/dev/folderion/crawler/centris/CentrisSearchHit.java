package dev.folderion.crawler.centris;

/**
 * One listing discovered on a Centris search results page.
 */
public record CentrisSearchHit(String id, String url, String title) {
}
