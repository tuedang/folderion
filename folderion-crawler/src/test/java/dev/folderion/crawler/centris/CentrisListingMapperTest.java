package dev.folderion.crawler.centris;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folderion.centris.CentrisListing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CentrisListingMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CentrisListingMapper listingMapper = new CentrisListingMapper();

    @Test
    void mapsExtractedJsonToCentrisListing() throws Exception {
        String json = """
                {
                  "id": "17351555",
                  "title": "House for sale",
                  "address_raw": "3895, Rue Outremont, Brossard",
                  "lat": "45.434909",
                  "lng": "-73.460514",
                  "price_raw": "$750,000",
                  "price_amount": "750000",
                  "rooms": "9 rooms",
                  "bedrooms": "4 bedrooms (1 in basement)",
                  "bathrooms": "2 bathrooms and 1 powder room",
                  "description": "Turnkey end-unit townhouse built in 2017!",
                  "features": [
                    {"name": "Building style", "value": "Two or more storey, Semi-detached"},
                    {"name": "Year built", "value": "2017"},
                    {"name": "Parking (total)", "value": "Driveway (2), Garage (1)"},
                    {"name": "Additional features", "value": "Basement 6 feet or +"},
                    {"name": "Move-in date", "value": "120 days after acceptance of promise to purchase or rent"}
                  ],
                  "financial_rows": [
                    {"label": "Lot", "value": "$158,100"},
                    {"label": "Building", "value": "$489,600"},
                    {"label": "Total", "value": "$647,700"},
                    {"label": "Municipal (2026)", "value": "$3,346"},
                    {"label": "School (2026)", "value": "$456"},
                    {"label": "Total", "value": "$3,802"}
                  ],
                  "brokers": [
                    {
                      "name": "Kristina Robinson-Palermo",
                      "role": "Residential Real Estate Broker",
                      "agency": "RE/MAX PLATINE Real Estate Agency"
                    }
                  ]
                }
                """;
        JsonNode extracted = mapper.readTree(json);
        String url = "https://www.centris.ca/en/houses~for-sale~brossard/17351555";

        CentrisListing listing = listingMapper.map(extracted, url);

        assertEquals("17351555", listing.getId());
        assertEquals("House for sale", listing.getTitle());
        assertEquals("3895, Rue Outremont", listing.getAddress().getStreet());
        assertEquals("Brossard", listing.getAddress().getCity());
        assertEquals(45.434909, listing.getAddress().getLat());
        assertEquals(750000, listing.getPrice().getAmount());
        assertEquals(2017, listing.getFeatures().getYearBuilt());
        assertEquals(4, listing.getFeatures().getBedrooms());
        assertEquals("1 in basement", listing.getFeatures().getBedroomsNote());
        assertEquals(2, listing.getFeatures().getBathrooms());
        assertEquals(1, listing.getFeatures().getPowderRooms());
        assertEquals(158100, listing.getFinancial().getMunicipalAssessment2026().getLot());
        assertEquals(3346, listing.getFinancial().getTaxesYearly().getMunicipal());
        assertEquals(1, listing.getBrokers().size());
        assertEquals("Kristina Robinson-Palermo", listing.getBrokers().getFirst().getName());
        assertEquals(url, listing.sourceUrl());
        assertNotNull(listing.getDescription());
    }
}
