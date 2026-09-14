package dev.folderion.crawler.centris;

import dev.folderion.centris.CentrisBucket;
import dev.folderion.centris.CentrisListing;
import dev.folderion.centris.CentrisWriter;
import dev.folderion.core.Bucket;
import dev.folderion.core.CommitResult;
import dev.folderion.crawler.crawl4ai.Crawl4AiClient;
import dev.folderion.crawler.crawl4ai.Crawl4AiConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * End-to-end job: Centris search → listing detail crawl → {@link CentrisBucket} write.
 */
public final class CentrisCrawlJob {

    private final CentrisSearchCrawler searchCrawler;
    private final CentrisListingCrawler listingCrawler;

    public CentrisCrawlJob(Crawl4AiClient client) {
        this(new CentrisSearchCrawler(client), new CentrisListingCrawler(client));
    }

    public CentrisCrawlJob(CentrisSearchCrawler searchCrawler, CentrisListingCrawler listingCrawler) {
        this.searchCrawler = Objects.requireNonNull(searchCrawler, "searchCrawler");
        this.listingCrawler = Objects.requireNonNull(listingCrawler, "listingCrawler");
    }

    public static CentrisCrawlJob createDefault() {
        return new CentrisCrawlJob(new Crawl4AiClient(Crawl4AiConfig.defaults()));
    }

    public Report run(String searchUrl, Path bucketRoot, int maxListings) {
        return run(searchUrl, bucketRoot, maxListings, false);
    }

    /**
     * @param paginate when {@code true}, crawl every search results page ({@code page=1..N})
     */
    public Report run(String searchUrl, Path bucketRoot, int maxListings, boolean paginate) {
        Objects.requireNonNull(searchUrl, "searchUrl");
        Objects.requireNonNull(bucketRoot, "bucketRoot");
        if (maxListings < 1) {
            throw new IllegalArgumentException("maxListings must be >= 1");
        }

        List<CentrisSearchHit> hits = searchCrawler.crawl(searchUrl, paginate);
        if (hits.size() > maxListings) {
            hits = hits.subList(0, maxListings);
        }

        List<CommitResult> commits = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        try (Bucket bucket = openOrInit(bucketRoot)) {
            CentrisWriter writer = new CentrisWriter(bucket);
            for (CentrisSearchHit hit : hits) {
                try {
                    CentrisListing listing = listingCrawler.crawl(hit.url());
                    CommitResult result = writer.write(listing, List.of());
                    commits.add(result);
                    System.out.printf("OK  %s  %s  %s%n", listing.getId(), result.status(), hit.url());
                } catch (RuntimeException e) {
                    failures.add(hit.id() + ": " + e.getMessage());
                    System.err.printf("FAIL %s  %s%n", hit.id(), e.getMessage());
                }
            }
        }

        return new Report(hits.size(), commits.size(), List.copyOf(failures));
    }

    private static Bucket openOrInit(Path bucketRoot) {
        if (Files.isRegularFile(bucketRoot.resolve("bucket.json"))) {
            return CentrisBucket.INSTANCE.open(bucketRoot);
        }
        return CentrisBucket.INSTANCE.init(bucketRoot);
    }

    public record Report(int discovered, int written, List<String> failures) {
    }
}
