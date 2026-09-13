package dev.folderion.centris;

import java.util.List;

/**
 * Offline Centris listing fixtures for tests (no live HTTP).
 */
public final class CentrisTestFixtures {

    private CentrisTestFixtures() {
    }

    /** Source: https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461 */
    public static CentrisListing listing27481461() {
        return CentrisListing.builder()
                .id("27481461")
                .title("Condominium house for sale")
                .address(CentrisListing.Address.builder()
                        .street("4334, Rue des Montgolfières")
                        .city("Longueuil (Saint-Hubert)")
                        .region("Montérégie")
                        .lat(45.523876)
                        .lng(-73.429572)
                        .build())
                .price(CentrisListing.Price.builder()
                        .amount(688800)
                        .currency("CAD")
                        .raw("$688,800")
                        .build())
                .features(CentrisListing.Features.builder()
                        .rooms(12)
                        .bedrooms(4)
                        .bedroomsNote("1 in basement")
                        .bathrooms(1)
                        .powderRooms(1)
                        .condominiumType("Divided")
                        .buildingStyle("Attached")
                        .yearBuilt(2017)
                        .parking("Garage (2)")
                        .pool(List.of("Heated", "Inground"))
                        .additional(List.of("Basement 6 feet or +"))
                        .moveIn("30 days after acceptance of promise to purchase or rent")
                        .build())
                .financial(CentrisListing.Financial.builder()
                        .municipalAssessment2026(CentrisListing.Assessment.builder()
                                .lot(201300)
                                .building(373700)
                                .total(575000)
                                .build())
                        .taxesYearly(CentrisListing.TaxesYearly.builder()
                                .municipal(4430)
                                .school(394)
                                .total(4824)
                                .build())
                        .condoFeesMonthly(277)
                        .condoFeesYearly(3324)
                        .build())
                .description(
                        "Beautiful divided co-ownership home featuring high-end finishes and a thoughtfully designed interior.")
                .brokers(List.of(
                        CentrisListing.Broker.builder()
                                .name("Alexis Roux-Spitz")
                                .role("Residential Real Estate Broker")
                                .agency("REALTA AGENCE IMMOBILIÈRE INC.")
                                .build(),
                        CentrisListing.Broker.builder()
                                .name("Alekos Fiorillo-Laroche")
                                .role("Residential Real Estate Broker")
                                .agency("REALTA AGENCE IMMOBILIÈRE INC.")
                                .build()))
                .source(CentrisListing.Source.builder()
                        .site("centris.ca")
                        .centrisNo("27481461")
                        .url("https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461")
                        .build())
                .media(CentrisListing.MediaMeta.builder()
                        .imageCount(26)
                        .build())
                .build();
    }

    /** Source: https://www.centris.ca/en/houses~for-sale~brossard/17351555 */
    public static CentrisListing listing17351555() {
        return CentrisListing.builder()
                .id("17351555")
                .title("House for sale")
                .address(CentrisListing.Address.builder()
                        .street("3895, Rue Outremont")
                        .city("Brossard")
                        .region("Montérégie")
                        .lat(45.434909)
                        .lng(-73.460514)
                        .build())
                .price(CentrisListing.Price.builder()
                        .amount(750000)
                        .currency("CAD")
                        .raw("$750,000")
                        .build())
                .features(CentrisListing.Features.builder()
                        .rooms(9)
                        .bedrooms(4)
                        .bedroomsNote("1 in basement")
                        .bathrooms(2)
                        .powderRooms(1)
                        .buildingStyle("Two or more storey, Semi-detached")
                        .yearBuilt(2017)
                        .parking("Driveway (2), Garage (1)")
                        .additional(List.of("Basement 6 feet or +"))
                        .moveIn("120 days after acceptance of promise to purchase or rent")
                        .build())
                .financial(CentrisListing.Financial.builder()
                        .municipalAssessment2026(CentrisListing.Assessment.builder()
                                .lot(158100)
                                .building(489600)
                                .total(647700)
                                .build())
                        .taxesYearly(CentrisListing.TaxesYearly.builder()
                                .municipal(3346)
                                .school(459)
                                .total(3805)
                                .build())
                        .build())
                .description(
                        "Turnkey end-unit townhouse built in 2017! This superb property features a garage and a private, "
                                + "non-shared driveway. The open-concept main floor impresses with a modern kitchen featuring a "
                                + "spacious 8 ft x 3 ft waterfall quartz island, a living room with a built-in TV unit, and a "
                                + "bright dining area. Upstairs: 3 bedrooms, a walk-in closet, a bathroom, and a convenient "
                                + "laundry area. The basement offers a 4th bedroom with an ensuite bathroom. Yard with shed, "
                                + "within walking distance of Parc Sainte-Marie and near the REM, DIX30, and major highways. "
                                + "A must-see!")
                .brokers(List.of(
                        CentrisListing.Broker.builder()
                                .name("Kristina Robinson-Palermo")
                                .role("Residential Real Estate Broker")
                                .agency("RE/MAX PLATINE")
                                .build(),
                        CentrisListing.Broker.builder()
                                .name("Marc Charbonneau")
                                .role("Certified Residential and Commercial Real Estate Broker AEO")
                                .agency("RE/MAX PLATINE M.C.")
                                .build()))
                .source(CentrisListing.Source.builder()
                        .site("centris.ca")
                        .centrisNo("17351555")
                        .url("https://www.centris.ca/en/houses~for-sale~brossard/17351555")
                        .build())
                .media(CentrisListing.MediaMeta.builder()
                        .imageCount(33)
                        .build())
                .build();
    }
}
