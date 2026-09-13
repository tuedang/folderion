package dev.folderion.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional map from JSON dotted paths to table column names.
 *
 * <p>Example: {@code price.amount → price}, {@code address.city → city}.
 * When omitted, {@link BucketQuery} auto-flattens leaf fields and uses the dotted path as the
 * column name.
 *
 * <p>Also build from SQL-like selectors: {@code "features.year_built as year"}, {@code "price.amount"}.
 */
public final class FieldMapping {

    private static final Pattern AS_ALIAS = Pattern.compile("(?i)^(.+?)\\s+as\\s+(\\S+)$");

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

    /**
     * Parse field selectors into a mapping.
     *
     * <pre>{@code
     * FieldMapping.select(List.of(
     *         "id",
     *         "title",
     *         "features.year_built as year",
     *         "price.amount"));
     * }</pre>
     *
     * <p>Without {@code as}, the column name is the path itself.
     */
    public static FieldMapping select(List<String> selectors) {
        Objects.requireNonNull(selectors, "selectors");
        Builder builder = builder();
        for (String selector : selectors) {
            parseSelector(selector, builder);
        }
        return builder.build();
    }

    public static FieldMapping select(String... selectors) {
        Objects.requireNonNull(selectors, "selectors");
        return select(List.of(selectors));
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

    private static void parseSelector(String selector, Builder builder) {
        Objects.requireNonNull(selector, "selector");
        String trimmed = selector.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("field selector must not be blank");
        }
        Matcher matcher = AS_ALIAS.matcher(trimmed);
        if (matcher.matches()) {
            builder.map(matcher.group(1).trim(), matcher.group(2).trim());
            return;
        }
        builder.map(trimmed, trimmed);
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
