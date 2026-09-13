package dev.folderion.centris;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folderion.core.Bucket;
import dev.folderion.core.CommitResult;
import dev.folderion.core.MediaBlob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uses parsed Centris listing 27481461 as fixture data (no live HTTP).
 * Source page: https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461
 */
class CentrisListingRecordTest {

    private static final String CENTRIS_NO = "27481461";
    private static final String SOURCE_URL =
            "https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461";

    /** Project build dir — inspectable after tests; cleaned before each run. */
    private final Path tempDir = Path.of("build").toAbsolutePath().normalize();

    @BeforeEach
    void cleanBuildBuckets() throws IOException {
        deleteRecursively(tempDir.resolve("buckets"));
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

    @Test
    void commitsCentrisListingFolderLayout() throws Exception {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisListing listing = CentrisTestFixtures.listing27481461();
            List<MediaBlob> images = List.of(
                    MediaBlob.image("01.jpg", jpegStub(1), "https://cdn.example/centris/27481461/01.jpg", 1),
                    MediaBlob.image("02.jpg", jpegStub(2), "https://cdn.example/centris/27481461/02.jpg", 2),
                    MediaBlob.image("03.jpg", jpegStub(3), "https://cdn.example/centris/27481461/03.jpg", 3)
            );

            CommitResult created = new CentrisWriter(bucket).write(listing, images);

            assertEquals(CommitResult.Status.CREATED, created.status());
            assertEquals(1, created.version());
            Path recordDir = created.recordDir();
            assertEquals(
                    tempDir.resolve("buckets/centris/centris-27481461/v1/content").toAbsolutePath().normalize(),
                    recordDir.toAbsolutePath().normalize());
            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/centris-27481461")));
            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/centris-27481461/v1")));
            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/bucket.json")));
            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/schemas")));
            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris-work")));
            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/0=ocfl_1.1")));
            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/ocfl_layout.json")));
            assertFalse(Files.exists(tempDir.resolve("buckets/centris/ocfl_1.1.md")));
            assertFalse(Files.exists(tempDir.resolve("buckets/centris/0002-flat-direct-storage-layout.md")));
            assertFalse(Files.isDirectory(tempDir.resolve("buckets/centris-ocfl")));

            assertTrue(Files.isRegularFile(recordDir.resolve("record.json")));
            assertTrue(Files.isRegularFile(recordDir.resolve("recordme.md")));
            assertTrue(Files.isRegularFile(recordDir.resolve("original.url")));
            assertTrue(Files.isRegularFile(recordDir.resolve("media/images/01.jpg")));
            assertTrue(Files.isRegularFile(recordDir.resolve("media/images/manifest.json")));

            CentrisReader reader = new CentrisReader(bucket);
            assertEquals(SOURCE_URL, reader.sourceUrl(CENTRIS_NO).orElseThrow());

            CentrisListing loaded = reader.read(CENTRIS_NO).orElseThrow();
            assertEquals(CENTRIS_NO, loaded.getId());
            assertEquals(688800, loaded.getPrice().getAmount());
            assertEquals(3, loaded.imageRefs().size());

            JsonNode record = new ObjectMapper().readTree(recordDir.resolve("record.json").toFile());
            assertEquals("centris.listing", record.get("record_type").asText());
            assertTrue(record.has("integrity"));

            assertEquals(3, reader.imageManifest(CENTRIS_NO).orElseThrow().getCount());
            assertEquals(3, reader.imagePaths(CENTRIS_NO).size());

            String readme = reader.readme(CENTRIS_NO).orElseThrow();
            assertTrue(readme.contains("27481461"));
            assertTrue(readme.contains("$688,800"));
        }
    }

    @Test
    void secondCommitWithSamePayloadIsUnchanged() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            List<MediaBlob> images = sampleImages(2);

            assertEquals(CommitResult.Status.CREATED, writer.write(CentrisTestFixtures.listing27481461(), images).status());
            CommitResult again = writer.write(CentrisTestFixtures.listing27481461(), images);
            assertEquals(CommitResult.Status.UNCHANGED, again.status());
            assertEquals(List.of(1), bucket.listVersions(CENTRIS_NO));
        }
    }

    @Test
    void priceChangeCreatesNewOcflVersion() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            CentrisReader reader = new CentrisReader(bucket);
            List<MediaBlob> images = sampleImages(2);

            assertEquals(CommitResult.Status.CREATED, writer.write(CentrisTestFixtures.listing27481461(), images).status());

            CentrisListing changed = CentrisTestFixtures.listing27481461();
            changed.getPrice().setAmount(679000);
            changed.getPrice().setRaw("$679,000");
            CommitResult updated = writer.write(changed, images);

            assertEquals(CommitResult.Status.UPDATED, updated.status());
            assertEquals(2, updated.version());
            assertEquals(List.of(1, 2), bucket.listVersions(CENTRIS_NO));
            assertEquals(688800, bucket.readVersionRecord(CENTRIS_NO, 1).orElseThrow()
                    .get("price").get("amount").asInt());
            assertEquals(679000, reader.read(CENTRIS_NO).orElseThrow().getPrice().getAmount());

            var timeline = reader.priceHistory(CENTRIS_NO);
            assertEquals(2, timeline.size());
            assertEquals("v1", timeline.get(0).versionId());
            assertEquals(688800, timeline.get(0).price().getAmount());
            assertEquals("v2", timeline.get(1).versionId());
            assertEquals(679000, timeline.get(1).price().getAmount());
        }
    }

    @Test
    void ocflKeepsFullVersionHistory() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            List<MediaBlob> images = sampleImages(1);

            int[] prices = {600000, 610000, 620000, 630000, 640000};
            for (int price : prices) {
                CentrisListing listing = CentrisTestFixtures.listing27481461();
                listing.getPrice().setAmount(price);
                listing.getPrice().setRaw("$" + price);
                writer.write(listing, images);
            }

            assertEquals(List.of(1, 2, 3, 4, 5), bucket.listVersions(CENTRIS_NO));
            assertEquals(600000, bucket.readVersionRecord(CENTRIS_NO, 1).orElseThrow()
                    .get("price").get("amount").asInt());
            assertEquals(640000, new CentrisReader(bucket).read(CENTRIS_NO).orElseThrow()
                    .getPrice().getAmount());
        }
    }

    private static List<MediaBlob> sampleImages(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(n -> MediaBlob.image(
                        String.format("%02d.jpg", n),
                        jpegStub(n),
                        "https://cdn.example/centris/27481461/" + String.format("%02d.jpg", n),
                        n))
                .toList();
    }

    private static byte[] jpegStub(int n) {
        return ("fake-jpeg-centris-27481461-" + n).getBytes(StandardCharsets.UTF_8);
    }
}
