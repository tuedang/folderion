package dev.folderion.centris;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture listing 17351555 (Brossard).
 * Source: https://www.centris.ca/en/houses~for-sale~brossard/17351555
 */
class CentrisListing17351555Test {

    private static final String CENTRIS_NO = "17351555";
    private static final String SOURCE_URL =
            "https://www.centris.ca/en/houses~for-sale~brossard/17351555";

    private final Path tempDir = Path.of("build").toAbsolutePath().normalize();

    @BeforeEach
    void cleanBuildBuckets() throws IOException {
        deleteRecursively(tempDir.resolve("buckets"));
    }

    @Test
    void commitsBrossardListing() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/centris"))) {
            CentrisListing listing = CentrisTestFixtures.listing17351555();
            List<MediaBlob> images = List.of(
                    MediaBlob.image("01.jpg", jpegStub(1), "https://cdn.example/centris/17351555/01.jpg", 1),
                    MediaBlob.image("02.jpg", jpegStub(2), "https://cdn.example/centris/17351555/02.jpg", 2)
            );

            CommitResult created = new CentrisWriter(bucket).write(listing, images);
            assertEquals(CommitResult.Status.CREATED, created.status());

            CentrisReader reader = new CentrisReader(bucket);
            CentrisListing loaded = reader.read(CENTRIS_NO).orElseThrow();

            assertEquals(CENTRIS_NO, loaded.getId());
            assertEquals("House for sale", loaded.getTitle());
            assertEquals(2017, loaded.getFeatures().getYearBuilt());
            assertEquals(750000, loaded.getPrice().getAmount());
            assertEquals(SOURCE_URL, reader.sourceUrl(CENTRIS_NO).orElseThrow());
            assertTrue(Files.isRegularFile(created.recordDir().resolve("record.json")));
        }
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

    private static byte[] jpegStub(int n) {
        return ("fake-jpeg-centris-17351555-" + n).getBytes(StandardCharsets.UTF_8);
    }
}
