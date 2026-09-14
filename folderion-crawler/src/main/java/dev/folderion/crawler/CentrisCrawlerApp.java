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
            "https://www.centris.ca/en/houses~for-sale?sort=DateDesc&q=H4sIAAAAAAAACo2U3W6bQBCFXwVxlUquRFynjtur2G2cqE5qhSpSf3IxhjGMurDu7JIURXn3DqY2eI1afAO7_ubscM7As58p47_zA3_gr1j_RJ7pGGVD1nq9pgg_YVkvC4Nz1AnDJi3DFDYodcHAN9XtPeGTLL8_yBqBo_QWskrlBij3IiaLTCAaa1JyW5HPfgY2Sr-Um4qbkS0_kLFMkRXM4m8ru6FU29cLyFbI1ju50bn9UQTBxwnXl4TwldAUCzsenfkvgz6qU11EKfIjKYVN9aRvNWtjgOOmctSzcgHekoGY9qee9z51ofOkwIKUd1KbclVUnrSefnyktAuLogtGaLQqG_8aCKJ3bRTk8V5pHs7D0dvh6T8bu0VK0pUuONU6DreBN_oHf3qfrXhtBp7r27f3_3PuQqlOA2TfW2nWRZKafdsiJbO3JlSxuQdVYD1j243r-NjXx4ppjU0PsM6qBzjqB573Vxw74FG0O_QgvW7h7ux2Ak0s3dV1KDvaJZesNzKW5TbNBgspTxReQkaqvNLyYXD0wWKiuWxV3KGhGHNLoBw4RKVEzj0B5F0-BO-0zkwLeVON_HDqUFOw6VI_xcguP-zk58CQYJs77eQW9ChdOumEvwpgvES0Dv1VYpjpXCwuIks6b8tPxkF_etL6uR3JW963n8rOpcTdtng0PgsCt5UucMsJ-PDyB_j7jaldBgAA&v=2&sortSeed=198967665&pageSize=20";

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
