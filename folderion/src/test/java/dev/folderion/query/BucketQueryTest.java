package dev.folderion.query;

import dev.folderion.centris.CentrisBucket;
import dev.folderion.centris.CentrisTestFixtures;
import dev.folderion.centris.CentrisWriter;
import dev.folderion.core.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

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
                    "price.amount"
            )).table();
//            addRealBedroomsColumn(table);
//            addPriceOffsetColumn(table);

            table.insertColumn(0, IntColumn.create("#", IntStream.rangeClosed(1, table.rowCount()).toArray()));

            System.out.println(table.printAll());

            assertEquals(2, table.rowCount());
            assertEquals(List.of("#", "id", "title", "year", "price.amount"), table.columnNames());
            assertEquals(List.of("17351555", "27481461"), table.stringColumn("id").asList());
            assertEquals("House for sale", table.stringColumn("title").get(0));
            assertEquals(2017L, table.longColumn("year").getLong(0));
            assertEquals(750000L, table.longColumn("price.amount").getLong(0));
            assertEquals("Condominium house for sale", table.stringColumn("title").get(1));
            assertEquals(688800L, table.longColumn("price.amount").getLong(1));
        }
    }

    @Test
    void listsCentrisItemsByCondition() {
        try (Bucket bucket = CentrisBucket.INSTANCE.open(tempDir.resolve("D:\\workspace\\folderion\\buckets\\centris"))) {
            Table table = new BucketQuery(bucket, List.of(
                    "id",
                    "features.year_built as year",
                    "price.amount",
                    "financial.municipal_assessment_2026.total as price.city",
                    "financial.fees_yearly.total as fees",
                    "features.bedrooms as bedrooms",
                    "features.bedrooms_note as bedrooms_note",
                    "features.bathrooms as bathrooms",
                    "address.street as address",
                    "address.city as city",
                    "source.url as url"
            )).table(-1); // HEAD + all older versions (each version once)
            addRealBedroomsColumn(table);
            addPriceOffsetColumn(table);
            table = table.where(table.intColumn("real_bedrooms").isGreaterThanOrEqualTo(3));
            table = table.where(table.longColumn("bathrooms").isGreaterThanOrEqualTo(2));
            table = table.dropWhere(table.stringColumn("city").containsString("Montréal"));
            table = table.dropWhere(table.stringColumn("city").isIn("Beaconsfield", "Pointe-Claire", "Côte-Saint-Luc", "Dollard-des-Ormeaux",
                    "Boucherville", "Dorval"));

            table = table.dropWhere(table.longColumn("fees").isGreaterThan(2100));
            table = table.dropWhere(table.stringColumn("address").containsString("Z, "));

            table = table.sortDescendingOn("year", "city", "bedrooms");

            table.insertColumn(0, IntColumn.create("#", IntStream.rangeClosed(1, table.rowCount()).toArray()));

            System.out.println(table.printAll());
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

    private static final Pattern BASEMENT_BEDROOMS =
            Pattern.compile("(?i)^(\\d+)\\s+in\\s+basement\\s*$");

    /**
     * {@code real_bedrooms = bedrooms - N} when note is {@code "N in basement"}; otherwise equals
     * {@code bedrooms}.
     */
    private static void addRealBedroomsColumn(Table table) {
        LongColumn bedrooms = table.longColumn("bedrooms");
        StringColumn notes = table.stringColumn("bedrooms_note");
        IntColumn real = IntColumn.create("real_bedrooms");
        for (int i = 0; i < table.rowCount(); i++) {
            if (bedrooms.isMissing(i)) {
                real.appendMissing();
                continue;
            }
            String note = notes.isMissing(i) ? null : notes.get(i);
            real.append(realBedrooms(bedrooms.getLong(i), note));
        }
        table.insertColumn(table.columnIndex("bedrooms") + 1, real);
    }

    /**
     * {@code price.offset = price.amount - price.city}, shown as {@code 55800 (12%)} where % is
     * relative to {@code price.city}.
     */
    private static void addPriceOffsetColumn(Table table) {
        LongColumn amount = table.longColumn("price.amount");
        LongColumn cityPrice = table.longColumn("price.city");
        StringColumn offset = StringColumn.create("price.offset");
        for (int i = 0; i < table.rowCount(); i++) {
            if (amount.isMissing(i) || cityPrice.isMissing(i)) {
                offset.appendMissing();
                continue;
            }
            long delta = amount.getLong(i) - cityPrice.getLong(i);
            long city = cityPrice.getLong(i);
            if (city == 0) {
                offset.append(Long.toString(delta));
            } else {
                long percent = Math.round(delta * 100.0 / city);
                offset.append(delta + " (" + percent + "%)");
            }
        }
        table.insertColumn(table.columnIndex("price.city") + 1, offset);
    }

    private static int realBedrooms(long bedrooms, String bedroomsNote) {
        Integer basement = parseBasementBedrooms(bedroomsNote);
        if (basement == null) {
            return Math.toIntExact(bedrooms);
        }
        return Math.toIntExact(bedrooms - basement);
    }

    /** @return basement bedroom count, or null if blank / unparseable */
    private static Integer parseBasementBedrooms(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        Matcher matcher = BASEMENT_BEDROOMS.matcher(note.trim());
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
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
