package dev.folderion.centris;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parsed Centris listing (DTO). Image bytes are not part of this model — see {@link CentrisImageRef}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CentrisListing {

    @NonNull
    private String id;
    private String title;
    private Address address;
    private Price price;
    private Features features;
    private Financial financial;
    private String description;
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private List<Broker> brokers = new ArrayList<>();
    @JsonProperty("open_houses")
    @Builder.Default
    @Setter(AccessLevel.NONE)
    private List<OpenHouse> openHouses = new ArrayList<>();
    private Source source;
    private MediaMeta media;

    @JsonIgnore
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<CentrisImageRef> imageRefs = new ArrayList<>();

    public List<CentrisImageRef> imageRefs() {
        return List.copyOf(imageRefs);
    }

    public void setImageRefs(List<CentrisImageRef> imageRefs) {
        this.imageRefs = imageRefs != null ? new ArrayList<>(imageRefs) : new ArrayList<>();
    }

    public String sourceUrl() {
        if (source != null && source.getUrl() != null) {
            return source.getUrl();
        }
        return null;
    }

    /**
     * Payload shape for {@code record.json} (fingerprint fields + source/media).
     */
    public Map<String, Object> toRecordPayload() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("title", title);
        root.put("address", address);
        root.put("price", price);
        root.put("features", features);
        root.put("financial", financial);
        root.put("description", description);
        root.put("brokers", brokers);
        root.put("open_houses", openHouses);
        if (source != null) {
            root.put("source", source);
        }
        if (media != null) {
            root.put("media", media);
        }
        return root;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Address {
        private String street;
        private String city;
        private String region;
        private Double lat;
        private Double lng;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Price {
        private Integer amount;
        private String currency;
        private String raw;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Features {
        private Integer rooms;
        private Integer bedrooms;
        @JsonProperty("bedrooms_note")
        private String bedroomsNote;
        private Integer bathrooms;
        @JsonProperty("powder_rooms")
        private Integer powderRooms;
        @JsonProperty("condominium_type")
        private String condominiumType;
        @JsonProperty("building_style")
        private String buildingStyle;
        @JsonProperty("year_built")
        private Integer yearBuilt;
        private String parking;
        private List<String> pool;
        private List<String> additional;
        @JsonProperty("move_in")
        private String moveIn;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Financial {
        @JsonProperty("municipal_assessment_2026")
        private Assessment municipalAssessment2026;
        @JsonProperty("taxes_yearly")
        private TaxesYearly taxesYearly;
        @JsonProperty("expenses_yearly")
        private ExpensesYearly expensesYearly;
        @JsonProperty("condo_fees_monthly")
        private Integer condoFeesMonthly;
        @JsonProperty("condo_fees_yearly")
        private Integer condoFeesYearly;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Assessment {
        private Integer lot;
        private Integer building;
        private Integer total;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TaxesYearly {
        private Integer municipal;
        private Integer school;
        private Integer total;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ExpensesYearly {
        private Integer electricity;
        private Integer gas;
        private Integer total;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Broker {
        private String name;
        private String role;
        private String agency;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class OpenHouse {
        private String date;
        private String start;
        private String end;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Source {
        private String site;
        @JsonProperty("centris_no")
        private String centrisNo;
        private String url;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MediaMeta {
        @JsonProperty("image_count")
        private Integer imageCount;
    }
}
