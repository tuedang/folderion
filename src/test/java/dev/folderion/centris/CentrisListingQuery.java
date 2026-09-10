package dev.folderion.centris;

import dev.folderion.core.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Query / catalog showcase over Centris listings in a bucket.
 */
class CentrisListingQuery {

    private final Path tempDir = Path.of("build").toAbsolutePath().normalize();

    @BeforeEach
    void cleanBuildBuckets() throws IOException {
        deleteRecursively(tempDir.resolve("buckets"));
    }

    /**
     * Showcase: seed two Centris records, then list id / title / year / price.
     */
    @Test
    void listsAllCentrisItems() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            writer.write(CentrisTestFixtures.listing27481461(), List.of());
            writer.write(CentrisTestFixtures.listing17351555(), List.of());

            CentrisReader reader = new CentrisReader(bucket);
            List<CentrisListing> all = reader.listAll();

            assertEquals(2, all.size());
            assertEquals(List.of("17351555", "27481461"), reader.listIds());

            printListingsTable(all);

            for (CentrisListing item : all) {
                assertTrue(item.getId() != null && !item.getId().isBlank());
                assertTrue(item.getTitle() != null && !item.getTitle().isBlank());
                assertTrue(item.getFeatures().getYearBuilt() != null && item.getFeatures().getYearBuilt() > 0);
                assertTrue(item.getPrice().getAmount() != null && item.getPrice().getAmount() > 0);
            }

            CentrisListing brossard = all.stream()
                    .filter(l -> "17351555".equals(l.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("House for sale", brossard.getTitle());
            assertEquals(2017, brossard.getFeatures().getYearBuilt());
            assertEquals(750000, brossard.getPrice().getAmount());
        }
    }

    private static void printListingsTable(List<CentrisListing> listings) {
        String idH = "id";
        String titleH = "title";
        String yearH = "year";
        String priceH = "price";

        int idW = Math.max(idH.length(), listings.stream().mapToInt(l -> l.getId().length()).max().orElse(0));
        int titleW = Math.max(titleH.length(), listings.stream().mapToInt(l -> l.getTitle().length()).max().orElse(0));
        int yearW = Math.max(yearH.length(), 4);
        int priceW = Math.max(priceH.length(), listings.stream()
                .mapToInt(l -> String.valueOf(l.getPrice().getRaw()).length())
                .max()
                .orElse(0));

        String rule = "+"
                + "-".repeat(idW + 2) + "+"
                + "-".repeat(titleW + 2) + "+"
                + "-".repeat(yearW + 2) + "+"
                + "-".repeat(priceW + 2) + "+";
        String fmt = "| %-" + idW + "s | %-" + titleW + "s | %-" + yearW + "s | %-" + priceW + "s |%n";

        System.out.println(rule);
        System.out.printf(fmt, idH, titleH, yearH, priceH);
        System.out.println(rule);
        for (CentrisListing item : listings) {
            System.out.printf(
                    fmt,
                    item.getId(),
                    item.getTitle(),
                    String.valueOf(item.getFeatures().getYearBuilt()),
                    item.getPrice().getRaw());
        }
        System.out.println(rule);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
