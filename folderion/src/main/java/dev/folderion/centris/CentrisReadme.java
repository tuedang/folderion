package dev.folderion.centris;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the listing {@code recordme.md} from a {@link CentrisListing}.
 */
final class CentrisReadme {

    private CentrisReadme() {
    }

    static String generate(CentrisListing listing) {
        CentrisListing.Address address = listing.getAddress();
        CentrisListing.Price price = listing.getPrice();
        CentrisListing.Features features = listing.getFeatures();
        String street = address != null && address.getStreet() != null ? address.getStreet() : "";
        String titleLine = listing.getTitle() != null ? shortTitle(listing.getTitle()) : "Listing";
        String priceRaw = price != null && price.getRaw() != null ? price.getRaw() : "";
        String currency = price != null && price.getCurrency() != null ? price.getCurrency() : "";
        String sourceUrl = listing.sourceUrl() != null ? listing.sourceUrl() : "";

        String city = address != null && address.getCity() != null ? address.getCity() : "";
        String region = address != null && address.getRegion() != null ? address.getRegion() : "";
        String location = joinNonBlank(", ", city, region);
        String coords = "";
        if (address != null && address.getLat() != null && address.getLng() != null) {
            coords = address.getLat() + ", " + address.getLng();
        }

        String bedrooms = formatBedrooms(features);
        String bathrooms = formatBathrooms(features);
        String pool = features != null && features.getPool() != null
                ? String.join(", ", features.getPool())
                : "";
        String additional = features != null && features.getAdditional() != null
                ? String.join(", ", features.getAdditional())
                : "";

        String descriptionLead = listing.getDescription() != null
                ? firstSentence(listing.getDescription())
                : "";

        return """
                # %s — %s

                **Centris No.** %s
                **Price:** %s %s
                **Source:** %s

                ## Location
                %s
                %s

                ## Features
                | Field | Value |
                |-------|--------|
                | Rooms | %s |
                | Bedrooms | %s |
                | Bathrooms | %s |
                | Condominium type | %s |
                | Building style | %s |
                | Year built | %s |
                | Parking | %s |
                | Pool | %s |
                | Additional | %s |
                | Move-in | %s |

                ## Description
                %s
                """.formatted(
                titleLine,
                street,
                nullToEmpty(listing.getId()),
                priceRaw,
                currency,
                sourceUrl,
                location,
                coords,
                features != null && features.getRooms() != null ? features.getRooms() : "",
                bedrooms,
                bathrooms,
                features != null ? nullToEmpty(features.getCondominiumType()) : "",
                features != null ? nullToEmpty(features.getBuildingStyle()) : "",
                features != null && features.getYearBuilt() != null ? features.getYearBuilt() : "",
                features != null ? nullToEmpty(features.getParking()) : "",
                pool,
                additional,
                features != null ? nullToEmpty(features.getMoveIn()) : "",
                descriptionLead
        ).stripTrailing() + "\n";
    }

    private static String shortTitle(String title) {
        // "Condominium house for sale" → "Condominium house"
        String trimmed = title.trim();
        if (trimmed.toLowerCase().endsWith(" for sale")) {
            return trimmed.substring(0, trimmed.length() - " for sale".length()).trim();
        }
        return trimmed;
    }

    private static String formatBedrooms(CentrisListing.Features features) {
        if (features == null || features.getBedrooms() == null) {
            return "";
        }
        if (features.getBedroomsNote() != null && !features.getBedroomsNote().isBlank()) {
            return features.getBedrooms() + " (" + features.getBedroomsNote() + ")";
        }
        return String.valueOf(features.getBedrooms());
    }

    private static String formatBathrooms(CentrisListing.Features features) {
        if (features == null) {
            return "";
        }
        Integer baths = features.getBathrooms();
        Integer powder = features.getPowderRooms();
        if (baths == null && powder == null) {
            return "";
        }
        if (powder != null && powder > 0) {
            return (baths != null ? baths : 0) + " bathroom + " + powder + " powder room";
        }
        return baths != null ? baths + " bathroom" : "";
    }

    private static String firstSentence(String text) {
        int end = text.indexOf('.');
        if (end >= 0 && end + 1 <= text.length()) {
            return text.substring(0, end + 1).trim();
        }
        return text.trim();
    }

    private static String joinNonBlank(String sep, String... parts) {
        return List.of(parts).stream()
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(sep));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
