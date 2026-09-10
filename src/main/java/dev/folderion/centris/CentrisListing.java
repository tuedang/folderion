package dev.folderion.centris;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed Centris listing (DTO). Image bytes are not part of this model — see {@link CentrisImageRef}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CentrisListing {

    private String id;
    private String title;
    private Address address;
    private Price price;
    private Features features;
    private Financial financial;
    private String description;
    private List<Broker> brokers = new ArrayList<>();
    @JsonProperty("open_houses")
    private List<OpenHouse> openHouses = new ArrayList<>();
    private Source source;
    private MediaMeta media;

    @JsonIgnore
    private List<CentrisImageRef> imageRefs = new ArrayList<>();

    public CentrisListing() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public Financial getFinancial() {
        return financial;
    }

    public void setFinancial(Financial financial) {
        this.financial = financial;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers != null ? new ArrayList<>(brokers) : new ArrayList<>();
    }

    public List<OpenHouse> getOpenHouses() {
        return openHouses;
    }

    public void setOpenHouses(List<OpenHouse> openHouses) {
        this.openHouses = openHouses != null ? new ArrayList<>(openHouses) : new ArrayList<>();
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public MediaMeta getMedia() {
        return media;
    }

    public void setMedia(MediaMeta media) {
        this.media = media;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final CentrisListing listing = new CentrisListing();

        public Builder id(String id) {
            listing.id = id;
            return this;
        }

        public Builder title(String title) {
            listing.title = title;
            return this;
        }

        public Builder address(Address address) {
            listing.address = address;
            return this;
        }

        public Builder price(Price price) {
            listing.price = price;
            return this;
        }

        public Builder features(Features features) {
            listing.features = features;
            return this;
        }

        public Builder financial(Financial financial) {
            listing.financial = financial;
            return this;
        }

        public Builder description(String description) {
            listing.description = description;
            return this;
        }

        public Builder brokers(List<Broker> brokers) {
            listing.setBrokers(brokers);
            return this;
        }

        public Builder openHouses(List<OpenHouse> openHouses) {
            listing.setOpenHouses(openHouses);
            return this;
        }

        public Builder source(Source source) {
            listing.source = source;
            return this;
        }

        public Builder media(MediaMeta media) {
            listing.media = media;
            return this;
        }

        public Builder imageRefs(List<CentrisImageRef> imageRefs) {
            listing.setImageRefs(imageRefs);
            return this;
        }

        public CentrisListing build() {
            Objects.requireNonNull(listing.id, "id");
            return listing;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Address {
        private String street;
        private String city;
        private String region;
        private Double lat;
        private Double lng;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Price {
        private Integer amount;
        private String currency;
        private String raw;

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getRaw() {
            return raw;
        }

        public void setRaw(String raw) {
            this.raw = raw;
        }
    }

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

        public Integer getRooms() {
            return rooms;
        }

        public void setRooms(Integer rooms) {
            this.rooms = rooms;
        }

        public Integer getBedrooms() {
            return bedrooms;
        }

        public void setBedrooms(Integer bedrooms) {
            this.bedrooms = bedrooms;
        }

        public String getBedroomsNote() {
            return bedroomsNote;
        }

        public void setBedroomsNote(String bedroomsNote) {
            this.bedroomsNote = bedroomsNote;
        }

        public Integer getBathrooms() {
            return bathrooms;
        }

        public void setBathrooms(Integer bathrooms) {
            this.bathrooms = bathrooms;
        }

        public Integer getPowderRooms() {
            return powderRooms;
        }

        public void setPowderRooms(Integer powderRooms) {
            this.powderRooms = powderRooms;
        }

        public String getCondominiumType() {
            return condominiumType;
        }

        public void setCondominiumType(String condominiumType) {
            this.condominiumType = condominiumType;
        }

        public String getBuildingStyle() {
            return buildingStyle;
        }

        public void setBuildingStyle(String buildingStyle) {
            this.buildingStyle = buildingStyle;
        }

        public Integer getYearBuilt() {
            return yearBuilt;
        }

        public void setYearBuilt(Integer yearBuilt) {
            this.yearBuilt = yearBuilt;
        }

        public String getParking() {
            return parking;
        }

        public void setParking(String parking) {
            this.parking = parking;
        }

        public List<String> getPool() {
            return pool;
        }

        public void setPool(List<String> pool) {
            this.pool = pool;
        }

        public List<String> getAdditional() {
            return additional;
        }

        public void setAdditional(List<String> additional) {
            this.additional = additional;
        }

        public String getMoveIn() {
            return moveIn;
        }

        public void setMoveIn(String moveIn) {
            this.moveIn = moveIn;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Financial {
        @JsonProperty("municipal_assessment_2026")
        private Assessment municipalAssessment2026;
        @JsonProperty("taxes_yearly")
        private TaxesYearly taxesYearly;
        @JsonProperty("condo_fees_monthly")
        private Integer condoFeesMonthly;
        @JsonProperty("condo_fees_yearly")
        private Integer condoFeesYearly;

        public Assessment getMunicipalAssessment2026() {
            return municipalAssessment2026;
        }

        public void setMunicipalAssessment2026(Assessment municipalAssessment2026) {
            this.municipalAssessment2026 = municipalAssessment2026;
        }

        public TaxesYearly getTaxesYearly() {
            return taxesYearly;
        }

        public void setTaxesYearly(TaxesYearly taxesYearly) {
            this.taxesYearly = taxesYearly;
        }

        public Integer getCondoFeesMonthly() {
            return condoFeesMonthly;
        }

        public void setCondoFeesMonthly(Integer condoFeesMonthly) {
            this.condoFeesMonthly = condoFeesMonthly;
        }

        public Integer getCondoFeesYearly() {
            return condoFeesYearly;
        }

        public void setCondoFeesYearly(Integer condoFeesYearly) {
            this.condoFeesYearly = condoFeesYearly;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Assessment {
        private Integer lot;
        private Integer building;
        private Integer total;

        public Integer getLot() {
            return lot;
        }

        public void setLot(Integer lot) {
            this.lot = lot;
        }

        public Integer getBuilding() {
            return building;
        }

        public void setBuilding(Integer building) {
            this.building = building;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TaxesYearly {
        private Integer municipal;
        private Integer school;
        private Integer total;

        public Integer getMunicipal() {
            return municipal;
        }

        public void setMunicipal(Integer municipal) {
            this.municipal = municipal;
        }

        public Integer getSchool() {
            return school;
        }

        public void setSchool(Integer school) {
            this.school = school;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Broker {
        private String name;
        private String role;
        private String agency;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getAgency() {
            return agency;
        }

        public void setAgency(String agency) {
            this.agency = agency;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class OpenHouse {
        private String date;
        private String start;
        private String end;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Source {
        private String site;
        @JsonProperty("centris_no")
        private String centrisNo;
        private String url;

        public String getSite() {
            return site;
        }

        public void setSite(String site) {
            this.site = site;
        }

        public String getCentrisNo() {
            return centrisNo;
        }

        public void setCentrisNo(String centrisNo) {
            this.centrisNo = centrisNo;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class MediaMeta {
        @JsonProperty("image_count")
        private Integer imageCount;

        public Integer getImageCount() {
            return imageCount;
        }

        public void setImageCount(Integer imageCount) {
            this.imageCount = imageCount;
        }
    }
}
