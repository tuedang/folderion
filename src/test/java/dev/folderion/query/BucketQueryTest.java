package dev.folderion.query;

import dev.folderion.centris.CentrisBucket;
import dev.folderion.centris.CentrisTestFixtures;
import dev.folderion.centris.CentrisWriter;
import dev.folderion.core.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BucketQueryTest {

    private final Path tempDir = Path.of("build").toAbsolutePath().normalize();

    @BeforeEach
    void cleanBuildBuckets() throws IOException {
        deleteRecursively(tempDir.resolve("buckets/query-test"));
    }

    /**
     * Showcase: seed two Centris records, then print id / title / year / price via {@link BucketQuery}.
     */
    @Test
    void listsAllCentrisItems() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/query-test/centris-list"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            writer.write(CentrisTestFixtures.listing27481461(), List.of());
            writer.write(CentrisTestFixtures.listing17351555(), List.of());

            Table table = new BucketQuery(bucket, List.of(
                    "id",
                    "title",
                    "features.year_built as year",
                    "price.amount")).table();
            System.out.println(table.print());

            assertEquals(2, table.rowCount());
            assertEquals(List.of("id", "title", "year", "price.amount"), table.columnNames());
            assertEquals(List.of("17351555", "27481461"), table.stringColumn("id").asList());
            assertEquals("House for sale", table.stringColumn("title").get(0));
            assertEquals(2017L, table.longColumn("year").getLong(0));
            assertEquals(750000L, table.longColumn("price.amount").getLong(0));
            assertEquals("Condominium house for sale", table.stringColumn("title").get(1));
            assertEquals(688800L, table.longColumn("price.amount").getLong(1));
        }
    }

    @Test
    void selectParsesAsAliasIntoMapping() {
        FieldMapping mapping = FieldMapping.select(
                "id",
                "title",
                "features.year_built as year",
                "price.amount");

        assertEquals(
                Map.of(
                        "id", "id",
                        "title", "title",
                        "features.year_built", "year",
                        "price.amount", "price.amount"),
                mapping.asMap());
    }

    @Test
    void projectsMappedColumns() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/query-test/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            writer.write(CentrisTestFixtures.listing27481461(), List.of());
            writer.write(CentrisTestFixtures.listing17351555(), List.of());

            FieldMapping mapping = FieldMapping.of(
                    "id", "id",
                    "title", "title",
                    "features.year_built", "year",
                    "price.amount", "price",
                    "address.city", "city");

            Table table = new BucketQuery(bucket, mapping).table();
            System.out.println(table.print());

            assertEquals(2, table.rowCount());
            assertEquals(List.of("id", "title", "year", "price", "city"), table.columnNames());
            assertEquals("17351555", table.stringColumn("id").get(0));
            assertEquals("House for sale", table.stringColumn("title").get(0));
            assertEquals(2017L, table.longColumn("year").getLong(0));
            assertEquals(750000L, table.longColumn("price").getLong(0));
            assertEquals("Brossard", table.stringColumn("city").get(0));
        }
    }

    @Test
    void autoFlattensWhenMappingOmitted() {
        try (Bucket bucket = CentrisBucket.INSTANCE.init(tempDir.resolve("buckets/query-test/centris-flat"))) {
            new CentrisWriter(bucket).write(CentrisTestFixtures.listing17351555(), List.of());

            Table table = new BucketQuery(bucket).table();
            System.out.println(table.print());

            assertEquals(1, table.rowCount());
            assertTrue(table.columnNames().contains("id"));
            assertTrue(table.columnNames().contains("title"));
            assertTrue(table.columnNames().contains("price.amount"));
            assertTrue(table.columnNames().contains("address.city"));
            assertTrue(table.columnNames().contains("features.year_built"));
            assertEquals(750000L, table.longColumn("price.amount").getLong(0));
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
}
