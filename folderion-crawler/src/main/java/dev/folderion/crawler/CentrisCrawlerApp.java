package dev.folderion.crawler;

import dev.folderion.crawler.centris.CentrisCrawlJob;
import dev.folderion.crawler.crawl4ai.Crawl4AiClient;
import dev.folderion.crawler.crawl4ai.Crawl4AiConfig;

import java.nio.file.Path;

/**
 * CLI entrypoint for Centris → Crawl4AI → {@code CentrisBucket}.
 *
 * <pre>
 * ./gradlew :folderion-crawler:run --args="--search-url URL --bucket buckets/centris"
 * </pre>
 */
public final class CentrisCrawlerApp {

    public static final String DEFAULT_SEARCH_URL =
            "https://www.centris.ca/en/houses~for-sale?q=H4sIAAAAAAAACo2UQW-bQBCF_wrilEquRFy7jptT7DZOVCe1QhUpbXMYwxhGXVh3d3GCovz3DiY2eI3a5WJ2-ebt8N7gFz8T2v_kB37PXyr5G9VUxsgbvJarFUX4Fct6WWicoUwUrNMyTGGNXBf0fF3d3hM-8fLnI68RVJTeQlap3ADlXqTIoCJgjRUJvq3IFz8DE6Xfy3XFTcmUn0kbRZFhzOCz4d2Qq837OWRLVMY7uZG5-VUEwZexqn8SwndMU8zsaDD0X3suqhNZRCmqDQmBTfXYtVpJrUHFTeXAsXIO3kIBKdqfeuZ86lzmSYEFCe-kNuWqqDxpvf3oSGkXFkUXCqHRqmx8MxBY71oLyOO90iychYOP_dN_NnaLlKRLWahUyjjcBt7oHzz0vhn2Wvc827cf5_9z7kKITgN431tKJYsk1fu2WYpnb0UoYn0PosB6xrYb1_Gxr5uKaY2NA1hn5QAO3MAzd8WRBR5Fu0MP0usW7s5uJ9DE0l1dh7KjbXKh5JrHstym2WAh5YnAS8hIlFeS_xgsfTCYSFW2Ku5QU4y5IRAWHKIQLGefAPwtH4J3Uma6hXyoRr4_sagJmHQhn2JUNt_v5GegIME2d9rJzWnDXVrphH8KUHiJaCz6gWOYypwtLiJDMm_Lj8eBOz1uXXZH_JW79lPZueC42xYPhwFfDuDoDXx8_Qtf-nEAXQYAAA&v=2&sortSeed=936090035&sort=DateDesc&pageSize=20&page=3";

    private CentrisCrawlerApp() {
    }

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        Crawl4AiConfig config = Crawl4AiConfig.defaults()
                .withBaseUrl(parsed.crawl4aiUrl)
                .withToken(parsed.token);
        CentrisCrawlJob job = new CentrisCrawlJob(new Crawl4AiClient(config));
        CentrisCrawlJob.Report report = job.run(parsed.searchUrl, parsed.bucket, parsed.maxListings);
        System.out.printf(
                "Done. discovered=%d written=%d failures=%d%n",
                report.discovered(), report.written(), report.failures().size());
        if (!report.failures().isEmpty()) {
            System.exit(1);
        }
    }

    private record Args(String searchUrl, Path bucket, String crawl4aiUrl, String token, int maxListings) {

        static Args parse(String[] args) {
            String searchUrl = DEFAULT_SEARCH_URL;
            Path bucket = Path.of("buckets/centris");
            String crawl4aiUrl = Crawl4AiConfig.DEFAULT_BASE_URL;
            String token = Crawl4AiConfig.DEFAULT_TOKEN;
            int maxListings = 20;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--search-url" -> searchUrl = requireValue(args, ++i, arg);
                    case "--bucket" -> bucket = Path.of(requireValue(args, ++i, arg));
                    case "--crawl4ai-url" -> crawl4aiUrl = requireValue(args, ++i, arg);
                    case "--token" -> token = requireValue(args, ++i, arg);
                    case "--max-listings" -> maxListings = Integer.parseInt(requireValue(args, ++i, arg));
                    case "--help", "-h" -> {
                        printHelp();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown arg: " + arg);
                }
            }
            return new Args(searchUrl, bucket, crawl4aiUrl, token, maxListings);
        }

        private static String requireValue(String[] args, int index, String flag) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return args[index];
        }

        private static void printHelp() {
            System.out.println("""
                    Centris crawler (Crawl4AI → CentrisBucket)

                    Options:
                      --search-url URL       Centris search results URL
                      --bucket PATH          Bucket directory (default: buckets/centris)
                      --crawl4ai-url URL     Crawl4AI base URL (default: http://192.168.68.57:11235)
                      --token TOKEN          Bearer token (default: mytoken)
                      --max-listings N       Max listings from the search page (default: 20)
                    """);
        }
    }
}
