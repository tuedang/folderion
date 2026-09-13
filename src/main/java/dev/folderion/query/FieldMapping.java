package dev.folderion.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional map from JSON dotted paths to table column names.
 *
 * <p>Example: {@code price.amount → price}, {@code address.city → city}.
 * When omitted, {@link BucketQuery} auto-flattens leaf fields and uses the dotted path as the
 * column name.
 */
public final class FieldMapping {

    private final Map<String, String> pathToColumn;

    private FieldMapping(Map<String, String> pathToColumn) {
        this.pathToColumn = Collections.unmodifiableMap(new LinkedHashMap<>(pathToColumn));
    }

    public static FieldMapping empty() {
        return new FieldMapping(Map.of());
    }

    /**
     * Alternating {@code path, column, path, column, ...}.
     *
     * <pre>{@code
     * FieldMapping.of("price.amount", "price", "address.city", "city");
     * }</pre>
     */
    public static FieldMapping of(String... pathAndColumnPairs) {
        Objects.requireNonNull(pathAndColumnPairs, "pathAndColumnPairs");
        if ((pathAndColumnPairs.length & 1) != 0) {
            throw new IllegalArgumentException("Expected path/column pairs, got odd length");
        }
        Builder builder = builder();
        for (int i = 0; i < pathAndColumnPairs.length; i += 2) {
            builder.map(pathAndColumnPairs[i], pathAndColumnPairs[i + 1]);
        }
        return builder.build();
    }

    /** Column name equals the JSON path for each entry. */
    public static FieldMapping paths(String... paths) {
        Objects.requireNonNull(paths, "paths");
        Builder builder = builder();
        for (String path : paths) {
            builder.map(path, path);
        }
        return builder.build();
    }

    public static FieldMapping from(Map<String, String> pathToColumn) {
        Objects.requireNonNull(pathToColumn, "pathToColumn");
        Builder builder = builder();
        pathToColumn.forEach(builder::map);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return pathToColumn.isEmpty();
    }

    /** Insertion-ordered path → column map. */
    public Map<String, String> asMap() {
        return pathToColumn;
    }

    public static final class Builder {
        private final Map<String, String> pathToColumn = new LinkedHashMap<>();

        private Builder() {
        }

        /** Map a dotted JSON path to a table column name. */
        public Builder map(String jsonPath, String columnName) {
            Objects.requireNonNull(jsonPath, "jsonPath");
            Objects.requireNonNull(columnName, "columnName");
            if (jsonPath.isBlank()) {
                throw new IllegalArgumentException("jsonPath must not be blank");
            }
            if (columnName.isBlank()) {
                throw new IllegalArgumentException("columnName must not be blank");
            }
            pathToColumn.put(jsonPath, columnName);
            return this;
        }

        public FieldMapping build() {
            return new FieldMapping(pathToColumn);
        }
    }
}
