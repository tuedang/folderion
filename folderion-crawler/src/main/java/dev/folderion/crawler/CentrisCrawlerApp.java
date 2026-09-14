package dev.folderion.crawler;

import dev.folderion.crawler.centris.CentrisCrawlJob;
import dev.folderion.crawler.crawl4ai.Crawl4AiClient;
import dev.folderion.crawler.crawl4ai.Crawl4AiConfig;

import java.nio.file.Path;
import java.util.Locale;

/**
 * CLI entrypoint for Centris → Crawl4AI → {@code CentrisBucket}.
 *
 * <pre>
 * ./gradlew :folderion-crawler:run --args="--search-url URL --bucket buckets/centris --paginate true"
 * </pre>
 */
public final class CentrisCrawlerApp {

    public static final String DEFAULT_SEARCH_URL =
            "https://www.centris.ca/en/houses~for-sale?sort=DateDesc&sortSeed=219314732&pageSize=20&q=H4sIAAAAAAAACo2U3W6bQBCFXwVxlUqu5LhOHbdXsds4UZ3UClWk_uRiDGMYdWHd2cUpivLuHUxs8BqlcGN2-ebscM7gJz9Vxv_g9_2ev2T9G3mqI5QNWevVikL8gkW1zA3OUMcM66QIElij1PV7vilv7wkfZfnzQdYIHCa3kJYqN0CZFzJZZALRWJGS25J88lOwYfKtWJfclGzxiYxlCq1gFv9a2Q2k2r6dQ7pEtt7Jjc7sr7zf_zzm6icmfCM0RcKOhmf-c6-L6kTnYYK8IaWwrh53rWZtDHBUVw47Vs7BWzAQ0_7U886nznUW55iT8k4qU67y0pPG24-OlHZhUXjBCLVWaeOLgSB610ZBFu2VZsEsGL4fnL7a2C1SnCx1zonWUbANvNY_eOh9teK16Xmubz8-_s-5C6VaDZB9b6lZ53Fi9m2LlMzeilBF5h5UjtWMbTeuo2NfNyXTGJsOYJVVB3DYDTzvrjhywKNod-hBeu3C7dntBOpY2qurUHa0Sy5Yr2Usi22aNRZQFiu8hJRUcaXlj8HRB4ux5qJRcYeGIswsgXLgAJUSOfcEkG_5ELzTOjUN5F058oOJQ03AJgv9GCG7_KCVnwFDjE3utJWb00a6dNIJ_uTAeIloHfq7xDDVmVich5Z01pQfj9wJfYUeNy63I_nKu_ZT2rmQuJsWn_XLqwN4_gI-PP8DDBvbqV0GAAA&v=2&view=Thumbnail";

    private CentrisCrawlerApp() {
    }

    public static void main(String[] args) {
        Args parsed = Args.parse(args);
        Crawl4AiConfig config = Crawl4AiConfig.defaults()
                .withBaseUrl(parsed.crawl4aiUrl)
                .withToken(parsed.token);
        CentrisCrawlJob job = new CentrisCrawlJob(new Crawl4AiClient(config));
        CentrisCrawlJob.Report report = job.run(
                parsed.searchUrl, parsed.bucket, parsed.maxListings, parsed.paginate);
        System.out.printf(
                "Done. discovered=%d written=%d failures=%d paginate=%s%n",
                report.discovered(), report.written(), report.failures().size(), parsed.paginate);
        if (!report.failures().isEmpty()) {
            System.exit(1);
        }
    }

    private record Args(
            String searchUrl,
            Path bucket,
            String crawl4aiUrl,
            String token,
            int maxListings,
            boolean paginate) {

        static Args parse(String[] args) {
            String searchUrl = DEFAULT_SEARCH_URL;
            Path bucket = Path.of("buckets/centris");
            String crawl4aiUrl = Crawl4AiConfig.DEFAULT_BASE_URL;
            String token = Crawl4AiConfig.DEFAULT_TOKEN;
            int maxListings = 20;
            boolean paginate = true;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--search-url" -> searchUrl = requireValue(args, ++i, arg);
                    case "--bucket" -> bucket = Path.of(requireValue(args, ++i, arg));
                    case "--crawl4ai-url" -> crawl4aiUrl = requireValue(args, ++i, arg);
                    case "--token" -> token = requireValue(args, ++i, arg);
                    case "--max-listings" -> maxListings = Integer.parseInt(requireValue(args, ++i, arg));
                    case "--paginate" -> paginate = parseBoolean(requireValue(args, ++i, arg), arg);
                    case "--help", "-h" -> {
                        printHelp();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown arg: " + arg);
                }
            }
            return new Args(searchUrl, bucket, crawl4aiUrl, token, maxListings, paginate);
        }

        private static String requireValue(String[] args, int index, String flag) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + flag);
            }
            return args[index];
        }

        private static boolean parseBoolean(String raw, String flag) {
            String value = raw.trim().toLowerCase(Locale.ROOT);
            return switch (value) {
                case "true", "1", "yes", "y" -> true;
                case "false", "0", "no", "n" -> false;
                default -> throw new IllegalArgumentException(
                        flag + " must be true or false, got: " + raw);
            };
        }

        private static void printHelp() {
            System.out.println("""
                    Centris crawler (Crawl4AI → CentrisBucket)

                    Options:
                      --search-url URL       Centris search results URL
                      --bucket PATH          Bucket directory (default: buckets/centris)
                      --crawl4ai-url URL     Crawl4AI base URL (default: http://192.168.68.57:11235)
                      --token TOKEN          Bearer token (default: mytoken)
                      --max-listings N       Max listings to write (default: 20)
                      --paginate true|false  Crawl all search pages page=1..N (default: false)
                                             Crawl4AI cache_mode=enabled on every request
                    """);
        }
    }
}
