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
                  "id": "23385250",
                  "title": "House for sale",
                  "address_raw": "86, Rue Elmo-Deslauriers, Sainte-Anne-de-Bellevue",
                  "lat": "45.408402",
                  "lng": "-73.953083",
                  "price_raw": "$739,900",
                  "price_amount": "739900",
                  "rooms": "15 rooms",
                  "bedrooms": "3 bedrooms",
                  "bathrooms": "2 bathrooms and 1 powder room",
                  "description": "Superb 3 floor townhouse condo located on quiet private street.",
                  "features": [
                    {"name": "Building style", "value": "Two or more storey, Attached"},
                    {"name": "Year built", "value": "2003"},
                    {"name": "Parking (total)", "value": "Driveway (3), Garage (1)"},
                    {"name": "Additional features", "value": "Basement 6 feet or +"},
                    {"name": "Move-in date", "value": "65 days after acceptance of promise to purchase or rent"}
                  ],
                  "financial_rows": [
                    {"label": "Lot", "value": "$144,900"},
                    {"label": "Building", "value": "$594,900"},
                    {"label": "Total", "value": "$739,800"},
                    {"label": "Municipal (2026)", "value": "$429"},
                    {"label": "School (2025)", "value": "$42"},
                    {"label": "Total", "value": "$471"},
                    {"label": "Municipal (2026)", "value": "$5,145"},
                    {"label": "School (2025)", "value": "$498"},
                    {"label": "Total", "value": "$5,643"},
                    {"label": "Common Expenses", "value": "$208"},
                    {"label": "Total", "value": "$208"},
                    {"label": "Common Expenses", "value": "$2,496"},
                    {"label": "Total", "value": "$2,496"},
                    {"label": "Electricity", "value": "$71"},
                    {"label": "Gas", "value": "$95"},
                    {"label": "Total", "value": "$166"},
                    {"label": "Electricity", "value": "$850"},
                    {"label": "Gas", "value": "$1,135"},
                    {"label": "Total", "value": "$1,985"}
                  ],
                  "brokers": [
                    {
                      "name": "Miao Hu",
                      "role": "Residential Real Estate Broker",
                      "agency": "RE/MAX ACTION"
                    }
                  ]
                }
                """;
        JsonNode extracted = mapper.readTree(json);
        String url = "https://www.centris.ca/en/houses~for-sale~sainte-anne-de-bellevue/23385250";

        CentrisListing listing = listingMapper.map(extracted, url);

        assertEquals("23385250", listing.getId());
        assertEquals("House for sale", listing.getTitle());
        assertEquals("86, Rue Elmo-Deslauriers", listing.getAddress().getStreet());
        assertEquals("Sainte-Anne-de-Bellevue", listing.getAddress().getCity());
        assertEquals(739900, listing.getPrice().getAmount());
        assertEquals(2003, listing.getFeatures().getYearBuilt());
        assertEquals(3, listing.getFeatures().getBedrooms());
        assertEquals(2, listing.getFeatures().getBathrooms());
        assertEquals(1, listing.getFeatures().getPowderRooms());
        assertEquals(144900, listing.getFinancial().getMunicipalAssessment2026().getLot());
        assertEquals(5145, listing.getFinancial().getTaxesYearly().getMunicipal());
        assertEquals(5643, listing.getFinancial().getTaxesYearly().getTotal());
        assertEquals(2496, listing.getFinancial().getFeesYearly().getCommonExpenses());
        assertEquals(2496, listing.getFinancial().getFeesYearly().getTotal());
        assertEquals(2496, listing.getFinancial().getCondoFeesYearly());
        assertEquals(850, listing.getFinancial().getExpensesYearly().getElectricity());
        assertEquals(1135, listing.getFinancial().getExpensesYearly().getGas());
        assertEquals(1985, listing.getFinancial().getExpensesYearly().getTotal());
        assertEquals("Miao Hu", listing.getBrokers().getFirst().getName());
        assertEquals(url, listing.sourceUrl());
        assertNotNull(listing.getDescription());
    }

    @Test
    void mapsCondominiumFeesYearlyLabel() throws Exception {
        // Centris uses "Condominium fees" (not "condo fees"); monthly+yearly both appear in DOM.
        String json = """
                {
                  "id": "25741571",
                  "title": "Condominium house for sale",
                  "address_raw": "651, Croissant de Namur, Saint-Lambert (Montérégie)",
                  "price_amount": "699000",
                  "financial_rows": [
                    {"label": "Lot", "value": "$149,000"},
                    {"label": "Building", "value": "$461,700"},
                    {"label": "Total", "value": "$610,700"},
                    {"label": "Municipal (2026)", "value": "$4,585"},
                    {"label": "School (2026)", "value": "$439"},
                    {"label": "Total", "value": "$5,024"},
                    {"label": "Condominium fees", "value": "$6,348"},
                    {"label": "Total", "value": "$6,348"},
                    {"label": "Electricity", "value": "$2,290"},
                    {"label": "Total", "value": "$2,290"}
                  ]
                }
                """;
        CentrisListing listing = listingMapper.map(
                mapper.readTree(json),
                "https://www.centris.ca/en/condominium-houses~for-sale~saint-lambert-monteregie/25741571");

        assertEquals(6348, listing.getFinancial().getFeesYearly().getCommonExpenses());
        assertEquals(6348, listing.getFinancial().getFeesYearly().getTotal());
        assertEquals(6348, listing.getFinancial().getCondoFeesYearly());
        assertEquals(2290, listing.getFinancial().getExpensesYearly().getElectricity());
    }
}
