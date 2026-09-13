package dev.folderion.crawler.centris;

import com.fasterxml.jackson.databind.JsonNode;
import dev.folderion.centris.CentrisListing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Crawl4AI JsonCss extraction output onto {@link CentrisListing}.
 */
public final class CentrisListingMapper {

    private static final Pattern FIRST_INT = Pattern.compile("(\\d+)");
    private static final Pattern MONEY = Pattern.compile("([\\d][\\d,]*)");

    public CentrisListing map(JsonNode extracted, String sourceUrl) {
        Objects.requireNonNull(extracted, "extracted");
        Objects.requireNonNull(sourceUrl, "sourceUrl");

        String id = firstNonBlank(text(extracted, "id"), text(extracted, "centris_no"), idFromUrl(sourceUrl));
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Extracted listing has no id for url=" + sourceUrl);
        }

        String title = text(extracted, "title");
        String addressRaw = text(extracted, "address_raw");
        Integer priceAmount = parseInt(text(extracted, "price_amount"));
        String priceRaw = text(extracted, "price_raw");
        if (priceAmount == null) {
            priceAmount = parseMoney(priceRaw);
        }

        Map<String, String> featureMap = featureMap(extracted.get("features"));
        CentrisListing.Features features = CentrisListing.Features.builder()
                .rooms(firstNonNull(parseInt(text(extracted, "rooms")), parseInt(featureMap.get("rooms"))))
                .bedrooms(parseBedrooms(text(extracted, "bedrooms"), featureMap))
                .bedroomsNote(parseBedroomsNote(text(extracted, "bedrooms")))
                .bathrooms(parseBathrooms(text(extracted, "bathrooms")))
                .powderRooms(parsePowderRooms(text(extracted, "bathrooms")))
                .buildingStyle(featureMap.get("building style"))
                .yearBuilt(parseInt(featureMap.get("year built")))
                .parking(featureMap.get("parking (total)"))
                .pool(splitList(featureMap.get("pool")))
                .additional(splitList(firstNonBlank(
                        featureMap.get("additional features"),
                        featureMap.get("additional feature"))))
                .moveIn(featureMap.get("move-in date"))
                .condominiumType(featureMap.get("condominium type"))
                .build();

        CentrisListing.Financial financial = mapFinancial(extracted.get("financial_rows"));
        List<CentrisListing.Broker> brokers = mapBrokers(extracted.get("brokers"));

        CentrisListing.Address address = parseAddress(addressRaw, extracted);

        return CentrisListing.builder()
                .id(id.trim())
                .title(title)
                .address(address)
                .price(CentrisListing.Price.builder()
                        .amount(priceAmount)
                        .currency("CAD")
                        .raw(priceRaw != null ? priceRaw : (priceAmount != null ? "$" + priceAmount : null))
                        .build())
                .features(features)
                .financial(financial)
                .description(blankToNull(text(extracted, "description")))
                .brokers(brokers)
                .source(CentrisListing.Source.builder()
                        .site("centris.ca")
                        .centrisNo(id.trim())
                        .url(sourceUrl)
                        .build())
                .build();
    }

    private static CentrisListing.Address parseAddress(String addressRaw, JsonNode extracted) {
        Double lat = parseDouble(text(extracted, "lat"));
        Double lng = parseDouble(text(extracted, "lng"));
        if (addressRaw == null || addressRaw.isBlank()) {
            return CentrisListing.Address.builder().lat(lat).lng(lng).build();
        }
        String trimmed = addressRaw.trim();
        int comma = trimmed.lastIndexOf(',');
        if (comma <= 0) {
            return CentrisListing.Address.builder().street(trimmed).lat(lat).lng(lng).build();
        }
        return CentrisListing.Address.builder()
                .street(trimmed.substring(0, comma).trim())
                .city(trimmed.substring(comma + 1).trim())
                .lat(lat)
                .lng(lng)
                .build();
    }

    private static Map<String, String> featureMap(JsonNode featuresNode) {
        Map<String, String> map = new LinkedHashMap<>();
        if (featuresNode == null || !featuresNode.isArray()) {
            return map;
        }
        for (JsonNode row : featuresNode) {
            String name = text(row, "name");
            String value = text(row, "value");
            if (name != null && value != null) {
                map.put(name.toLowerCase(Locale.ROOT), value);
            }
        }
        return map;
    }

    private static CentrisListing.Financial mapFinancial(JsonNode rowsNode) {
        if (rowsNode == null || !rowsNode.isArray()) {
            return null;
        }
        Integer lot = null;
        Integer building = null;
        Integer assessmentTotal = null;
        Integer municipalTax = null;
        Integer schoolTax = null;
        Integer taxTotal = null;

        // Heuristic: first Lot/Building/Total belong to assessment; later Municipal/School/Total to taxes.
        boolean inTaxes = false;
        for (JsonNode row : rowsNode) {
            String label = text(row, "label");
            String value = text(row, "value");
            if (label == null || value == null) {
                continue;
            }
            String key = label.toLowerCase(Locale.ROOT);
            Integer amount = parseMoney(value);
            if (amount == null) {
                continue;
            }
            if (key.contains("municipal") && key.contains("school")) {
                continue;
            }
            if (key.startsWith("municipal")) {
                municipalTax = amount;
                inTaxes = true;
            } else if (key.startsWith("school")) {
                schoolTax = amount;
                inTaxes = true;
            } else if (key.equals("lot")) {
                lot = amount;
            } else if (key.equals("building")) {
                building = amount;
            } else if (key.equals("total")) {
                if (inTaxes || municipalTax != null || schoolTax != null) {
                    taxTotal = amount;
                } else {
                    assessmentTotal = amount;
                }
            } else if (key.contains("condo fees") || key.contains("co-ownership fees")) {
                // leave for future; CentrisListing has condo fee fields
            }
        }

        if (lot == null && building == null && assessmentTotal == null
                && municipalTax == null && schoolTax == null && taxTotal == null) {
            return null;
        }

        CentrisListing.Assessment assessment = null;
        if (lot != null || building != null || assessmentTotal != null) {
            Integer total = assessmentTotal;
            if (total == null && lot != null && building != null) {
                total = lot + building;
            }
            assessment = CentrisListing.Assessment.builder()
                    .lot(lot)
                    .building(building)
                    .total(total)
                    .build();
        }
        CentrisListing.TaxesYearly taxes = null;
        if (municipalTax != null || schoolTax != null || taxTotal != null) {
            Integer total = taxTotal;
            if (total == null && municipalTax != null && schoolTax != null) {
                total = municipalTax + schoolTax;
            }
            taxes = CentrisListing.TaxesYearly.builder()
                    .municipal(municipalTax)
                    .school(schoolTax)
                    .total(total)
                    .build();
        }
        return CentrisListing.Financial.builder()
                .municipalAssessment2026(assessment)
                .taxesYearly(taxes)
                .build();
    }

    private static List<CentrisListing.Broker> mapBrokers(JsonNode brokersNode) {
        if (brokersNode == null || !brokersNode.isArray()) {
            return List.of();
        }
        List<CentrisListing.Broker> brokers = new ArrayList<>();
        for (JsonNode row : brokersNode) {
            String name = firstNonBlank(text(row, "name"), text(row, "name_aria"));
            if (name == null) {
                continue;
            }
            brokers.add(CentrisListing.Broker.builder()
                    .name(name)
                    .role(text(row, "role"))
                    .agency(text(row, "agency"))
                    .build());
        }
        return List.copyOf(brokers);
    }

    private static Integer parseBedrooms(String bedroomsText, Map<String, String> featureMap) {
        Integer fromTeaser = parseInt(bedroomsText);
        if (fromTeaser != null) {
            return fromTeaser;
        }
        return parseInt(featureMap.get("bedrooms"));
    }

    private static String parseBedroomsNote(String bedroomsText) {
        if (bedroomsText == null) {
            return null;
        }
        int open = bedroomsText.indexOf('(');
        int close = bedroomsText.indexOf(')');
        if (open >= 0 && close > open) {
            return bedroomsText.substring(open + 1, close).trim();
        }
        return null;
    }

    private static Integer parseBathrooms(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\s+bathroom", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return parseInt(text);
    }

    private static Integer parsePowderRooms(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\s+powder", Pattern.CASE_INSENSITIVE).matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static List<String> splitList(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split("[,;/]");
        List<String> values = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values.isEmpty() ? null : List.copyOf(values);
    }

    private static String idFromUrl(String url) {
        Matcher matcher = Pattern.compile("/(\\d{5,})/?$").matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static Integer parseInt(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = FIRST_INT.matcher(text.replace(",", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = MONEY.matcher(text.replace(" ", ""));
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
