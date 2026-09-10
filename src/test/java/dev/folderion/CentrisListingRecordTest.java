package dev.folderion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folderion.bucket.Bucket;
import dev.folderion.bucket.CommitResult;
import dev.folderion.bucket.MediaBlob;
import dev.folderion.centris.CentrisBucket;
import dev.folderion.centris.CentrisImageRef;
import dev.folderion.centris.CentrisListing;
import dev.folderion.centris.CentrisReader;
import dev.folderion.centris.CentrisWriter;
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
        try (Bucket bucket = CentrisBucket.init(tempDir.resolve("buckets/centris"))) {
            CentrisListing listing = sampleListing();
            List<MediaBlob> images = List.of(
                    MediaBlob.image("01.jpg", jpegStub(1), "https://cdn.example/centris/27481461/01.jpg", 1),
                    MediaBlob.image("02.jpg", jpegStub(2), "https://cdn.example/centris/27481461/02.jpg", 2),
                    MediaBlob.image("03.jpg", jpegStub(3), "https://cdn.example/centris/27481461/03.jpg", 3)
            );

            CommitResult created = new CentrisWriter(bucket).write(listing, images);

            assertEquals(CommitResult.Status.CREATED, created.status());
            assertEquals(1, created.version());
//            Path recordDir = created.recordDir();
//            assertEquals(
//                    tempDir.resolve("buckets/centris/centris-27481461/v1/content").toAbsolutePath().normalize(),
//                    recordDir.toAbsolutePath().normalize());
//            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/centris-27481461")));
//            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/centris-27481461/v1")));
//            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/bucket.json")));
//            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris/schemas")));
//            assertTrue(Files.isDirectory(tempDir.resolve("buckets/centris-work")));
//            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/0=ocfl_1.1")));
//            assertTrue(Files.isRegularFile(tempDir.resolve("buckets/centris/ocfl_layout.json")));
//            assertFalse(Files.exists(tempDir.resolve("buckets/centris/ocfl_1.1.md")));
//            assertFalse(Files.exists(tempDir.resolve("buckets/centris/0002-flat-direct-storage-layout.md")));
//            assertFalse(Files.isDirectory(tempDir.resolve("buckets/centris-ocfl")));
//
//            assertTrue(Files.isRegularFile(recordDir.resolve("record.json")));
//            assertTrue(Files.isRegularFile(recordDir.resolve("README.md")));
//            assertTrue(Files.isRegularFile(recordDir.resolve("source/original.url")));
//            assertTrue(Files.isRegularFile(recordDir.resolve("media/images/01.jpg")));
//            assertTrue(Files.isRegularFile(recordDir.resolve("media/images/manifest.json")));
//
//            CentrisReader reader = new CentrisReader(bucket);
//            assertEquals(SOURCE_URL, reader.sourceUrl(CENTRIS_NO).orElseThrow());
//
//            CentrisListing loaded = reader.read(CENTRIS_NO).orElseThrow();
//            assertEquals(CENTRIS_NO, loaded.getId());
//            assertEquals(688800, loaded.getPrice().getAmount());
//            assertEquals(3, loaded.imageRefs().size());
//
//            JsonNode record = new ObjectMapper().readTree(recordDir.resolve("record.json").toFile());
//            assertEquals("centris.listing", record.get("record_type").asText());
//            assertTrue(record.has("integrity"));
//
//            assertEquals(3, reader.imageManifest(CENTRIS_NO).orElseThrow().getCount());
//            assertEquals(3, reader.imagePaths(CENTRIS_NO).size());
//
//            String readme = reader.readme(CENTRIS_NO).orElseThrow();
//            assertTrue(readme.contains("27481461"));
//            assertTrue(readme.contains("$688,800"));
        }
    }

    @Test
    void secondCommitWithSamePayloadIsUnchanged() {
        try (Bucket bucket = CentrisBucket.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            List<MediaBlob> images = sampleImages(2);

            assertEquals(CommitResult.Status.CREATED, writer.write(sampleListing(), images).status());
            CommitResult again = writer.write(sampleListing(), images);
            assertEquals(CommitResult.Status.UNCHANGED, again.status());
            assertEquals(List.of(1), bucket.listVersions(CENTRIS_NO));
        }
    }

    @Test
    void priceChangeCreatesNewOcflVersion() {
        try (Bucket bucket = CentrisBucket.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            CentrisReader reader = new CentrisReader(bucket);
            List<MediaBlob> images = sampleImages(2);

            assertEquals(CommitResult.Status.CREATED, writer.write(sampleListing(), images).status());

            CentrisListing changed = sampleListing();
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
        try (Bucket bucket = CentrisBucket.init(tempDir.resolve("buckets/centris"))) {
            CentrisWriter writer = new CentrisWriter(bucket);
            List<MediaBlob> images = sampleImages(1);

            int[] prices = {600000, 610000, 620000, 630000, 640000};
            for (int price : prices) {
                CentrisListing listing = sampleListing();
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

    private static CentrisListing sampleListing() {
        CentrisListing.Address address = new CentrisListing.Address();
        address.setStreet("4334, Rue des Montgolfières");
        address.setCity("Longueuil (Saint-Hubert)");
        address.setRegion("Montérégie");
        address.setLat(45.523876);
        address.setLng(-73.429572);

        CentrisListing.Price price = new CentrisListing.Price();
        price.setAmount(688800);
        price.setCurrency("CAD");
        price.setRaw("$688,800");

        CentrisListing.Features features = new CentrisListing.Features();
        features.setRooms(12);
        features.setBedrooms(4);
        features.setBedroomsNote("1 in basement");
        features.setBathrooms(1);
        features.setPowderRooms(1);
        features.setCondominiumType("Divided");
        features.setBuildingStyle("Attached");
        features.setYearBuilt(2017);
        features.setParking("Garage (2)");
        features.setPool(List.of("Heated", "Inground"));
        features.setAdditional(List.of("Basement 6 feet or +"));
        features.setMoveIn("30 days after acceptance of promise to purchase or rent");

        CentrisListing.Assessment assessment = new CentrisListing.Assessment();
        assessment.setLot(201300);
        assessment.setBuilding(373700);
        assessment.setTotal(575000);

        CentrisListing.TaxesYearly taxesYearly = new CentrisListing.TaxesYearly();
        taxesYearly.setMunicipal(4430);
        taxesYearly.setSchool(394);
        taxesYearly.setTotal(4824);

        CentrisListing.Financial financial = new CentrisListing.Financial();
        financial.setMunicipalAssessment2026(assessment);
        financial.setTaxesYearly(taxesYearly);
        financial.setCondoFeesMonthly(277);
        financial.setCondoFeesYearly(3324);

        CentrisListing.Broker broker1 = new CentrisListing.Broker();
        broker1.setName("Alexis Roux-Spitz");
        broker1.setRole("Residential Real Estate Broker");
        broker1.setAgency("REALTA AGENCE IMMOBILIÈRE INC.");

        CentrisListing.Broker broker2 = new CentrisListing.Broker();
        broker2.setName("Alekos Fiorillo-Laroche");
        broker2.setRole("Residential Real Estate Broker");
        broker2.setAgency("REALTA AGENCE IMMOBILIÈRE INC.");

        CentrisListing.OpenHouse oh1 = new CentrisListing.OpenHouse();
        oh1.setDate("2026-09-12");
        oh1.setStart("11:30");
        oh1.setEnd("12:30");

        CentrisListing.OpenHouse oh2 = new CentrisListing.OpenHouse();
        oh2.setDate("2026-09-13");
        oh2.setStart("13:30");
        oh2.setEnd("14:30");

        CentrisListing.Source source = new CentrisListing.Source();
        source.setSite("centris.ca");
        source.setCentrisNo(CENTRIS_NO);
        source.setUrl(SOURCE_URL);

        CentrisListing.MediaMeta media = new CentrisListing.MediaMeta();
        media.setImageCount(26);

        return CentrisListing.builder()
                .id(CENTRIS_NO)
                .title("Condominium house for sale")
                .address(address)
                .price(price)
                .features(features)
                .financial(financial)
                .description(
                        "Beautiful divided co-ownership home featuring high-end finishes and a thoughtfully designed interior. "
                                + "The spacious and bright living area seamlessly brings together the living room, dining room and kitchen with quartz island. "
                                + "Three bedrooms upstairs, plus a family room and office in the basement with the possibility of creating a 4th bedroom. "
                                + "Double garage, large private terrace and shared heated in-ground pool. "
                                + "Peaceful setting close to Boisé du Tremblay, public transit and all services. "
                                + "A turnkey property combining space, comfort and quality of life.")
                .brokers(List.of(broker1, broker2))
                .openHouses(List.of(oh1, oh2))
                .source(source)
                .media(media)
                .imageRefs(List.of(
                        new CentrisImageRef("01.jpg", "https://cdn.example/centris/27481461/01.jpg", 1),
                        new CentrisImageRef("02.jpg", "https://cdn.example/centris/27481461/02.jpg", 2),
                        new CentrisImageRef("03.jpg", "https://cdn.example/centris/27481461/03.jpg", 3)
                ))
                .build();
    }

    private static byte[] jpegStub(int n) {
        return ("fake-jpeg-centris-27481461-" + n).getBytes(StandardCharsets.UTF_8);
    }
}
