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
            "https://www.centris.ca/en/houses~for-sale?sort=DateDesc&q=H4sIAAAAAAAACo2U3W7aQBCFX8XyVSpRyaGkhPYq0IZEJSmKq0j9uxjswR517aWz6yRWlHfvGBdsFqu1b_Au35wdzpnl2c-U8d_5gT_wV6x_Ic90jLIha71eU4SfsKyXhcE56oRhk5ZhChuUumDgm-r1nvBRlt9_yhqBo_QWskrlBij3IiaLTCAaa1LyWpHPfgY2Sr-Um4qbkS0_kLFMkRXM4pOV3VCq7esFZCtk653c6Nz-KILg44Trj4TwldAUCzsenfkvgz6qU11EKfIDKYVN9aRvNWtjgOOmctSzcgHekoGY9qee9z51ofOkwIKUd1KbclVUnrR-_fhIaRcWRReM0GhVNv41EETv2ijI473SPJyHo7fD0382douUpCtdcKp1HG4Db_QPvvQ-W_HaDDzXt2_v_-fchVKdBsi-t9KsiyQ1-7ZFSmZvTahicw-qwHrGthvX8bGvDxXTGpseYJ1VD3DUDzzvrzh2wKNod-hBet3C3dntBJpYuqvrUHa0Sy5Zb2Qsy22aDRZSnii8hIxUeaXlj8HRB4uJ5rJVcYeGYswtgXLgEJUSOfcEkLt8CN5pnZkW8qYa-eHUoaZg06V-jJFdftjJz4EhwTZ32skt6EG6dNIJfxfAeIloHfqrxDDTuVhcRJZ03pafjIP-9KT1uB3JLe_bT2XnUuJuW-x20cWMzwJ55Cq-_AG3ca4DWAYAAA&v=2&sortSeed=1506816405&pageSize=20";

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
            boolean paginate = false;

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
