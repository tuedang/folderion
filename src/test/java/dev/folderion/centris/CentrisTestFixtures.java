package dev.folderion.centris;

import java.util.List;

/**
 * Offline Centris listing fixtures for tests (no live HTTP).
 */
final class CentrisTestFixtures {

    private CentrisTestFixtures() {
    }

    /** Source: https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461 */
    static CentrisListing listing27481461() {
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

        CentrisListing.Source source = new CentrisListing.Source();
        source.setSite("centris.ca");
        source.setCentrisNo("27481461");
        source.setUrl("https://www.centris.ca/en/condominium-houses~for-sale~longueuil-saint-hubert/27481461");

        CentrisListing.MediaMeta media = new CentrisListing.MediaMeta();
        media.setImageCount(26);

        return CentrisListing.builder()
                .id("27481461")
                .title("Condominium house for sale")
                .address(address)
                .price(price)
                .features(features)
                .financial(financial)
                .description(
                        "Beautiful divided co-ownership home featuring high-end finishes and a thoughtfully designed interior.")
                .brokers(List.of(broker1, broker2))
                .source(source)
                .media(media)
                .build();
    }

    /** Source: https://www.centris.ca/en/houses~for-sale~brossard/17351555 */
    static CentrisListing listing17351555() {
        CentrisListing.Address address = new CentrisListing.Address();
        address.setStreet("3895, Rue Outremont");
        address.setCity("Brossard");
        address.setRegion("Montérégie");
        address.setLat(45.434909);
        address.setLng(-73.460514);

        CentrisListing.Price price = new CentrisListing.Price();
        price.setAmount(750000);
        price.setCurrency("CAD");
        price.setRaw("$750,000");

        CentrisListing.Features features = new CentrisListing.Features();
        features.setRooms(9);
        features.setBedrooms(4);
        features.setBedroomsNote("1 in basement");
        features.setBathrooms(2);
        features.setPowderRooms(1);
        features.setBuildingStyle("Two or more storey, Semi-detached");
        features.setYearBuilt(2017);
        features.setParking("Driveway (2), Garage (1)");
        features.setAdditional(List.of("Basement 6 feet or +"));
        features.setMoveIn("120 days after acceptance of promise to purchase or rent");

        CentrisListing.Assessment assessment = new CentrisListing.Assessment();
        assessment.setLot(158100);
        assessment.setBuilding(489600);
        assessment.setTotal(647700);

        CentrisListing.TaxesYearly taxesYearly = new CentrisListing.TaxesYearly();
        taxesYearly.setMunicipal(3346);
        taxesYearly.setSchool(459);
        taxesYearly.setTotal(3805);

        CentrisListing.Financial financial = new CentrisListing.Financial();
        financial.setMunicipalAssessment2026(assessment);
        financial.setTaxesYearly(taxesYearly);

        CentrisListing.Broker broker1 = new CentrisListing.Broker();
        broker1.setName("Kristina Robinson-Palermo");
        broker1.setRole("Residential Real Estate Broker");
        broker1.setAgency("RE/MAX PLATINE");

        CentrisListing.Broker broker2 = new CentrisListing.Broker();
        broker2.setName("Marc Charbonneau");
        broker2.setRole("Certified Residential and Commercial Real Estate Broker AEO");
        broker2.setAgency("RE/MAX PLATINE M.C.");

        CentrisListing.Source source = new CentrisListing.Source();
        source.setSite("centris.ca");
        source.setCentrisNo("17351555");
        source.setUrl("https://www.centris.ca/en/houses~for-sale~brossard/17351555");

        CentrisListing.MediaMeta media = new CentrisListing.MediaMeta();
        media.setImageCount(33);

        return CentrisListing.builder()
                .id("17351555")
                .title("House for sale")
                .address(address)
                .price(price)
                .features(features)
                .financial(financial)
                .description(
                        "Turnkey end-unit townhouse built in 2017! This superb property features a garage and a private, "
                                + "non-shared driveway. The open-concept main floor impresses with a modern kitchen featuring a "
                                + "spacious 8 ft x 3 ft waterfall quartz island, a living room with a built-in TV unit, and a "
                                + "bright dining area. Upstairs: 3 bedrooms, a walk-in closet, a bathroom, and a convenient "
                                + "laundry area. The basement offers a 4th bedroom with an ensuite bathroom. Yard with shed, "
                                + "within walking distance of Parc Sainte-Marie and near the REM, DIX30, and major highways. "
                                + "A must-see!")
                .brokers(List.of(broker1, broker2))
                .source(source)
                .media(media)
                .build();
    }
}
