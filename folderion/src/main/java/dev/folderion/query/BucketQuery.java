package dev.folderion.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import dev.folderion.core.Bucket;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Projects every record in a {@link Bucket} into a Tablesaw {@link Table} for display and later
 * filter / query.
 *
 * <p>Column selection is optional:
 * <ul>
 *   <li>none — auto-flatten leaf JSON fields (dotted path = column name)</li>
 *   <li>{@link FieldMapping} — explicit path → column map</li>
 *   <li>field selectors — e.g. {@code "features.year_built as year"}, {@code "price.amount"}</li>
 * </ul>
 *
 * <p>History:
 * <ul>
 *   <li>{@code 0} — HEAD only</li>
 *   <li>{@code 1} — HEAD + v1 (skips v1 if HEAD is already v1)</li>
 *   <li>{@code 2} — HEAD + v1 + v2 (skips the version that equals HEAD)</li>
 *   <li>{@code < 0} — HEAD + every older version (each version once)</li>
 * </ul>
 * When history is enabled, a {@code version} column is prepended (HEAD uses the head version number).
 * The HEAD row is never duplicated when the same OCFL version is also in the history range.
 *
 * <pre>{@code
 * Table table = new BucketQuery(bucket, List.of(
 *         "id",
 *         "title",
 *         "features.year_built as year",
 *         "price.amount")).table(2);
 * table.print();
 * }</pre>
 */
public final class BucketQuery {

    private final Bucket bucket;
    private final FieldMapping mapping;

    public BucketQuery(Bucket bucket) {
        this(bucket, (FieldMapping) null);
    }

    public BucketQuery(Bucket bucket, FieldMapping mapping) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.mapping = mapping == null || mapping.isEmpty() ? null : mapping;
    }

    /**
     * Select columns via field selectors ({@code path} or {@code path as alias}).
     * Parsed into a {@link FieldMapping} and delegated to {@link #BucketQuery(Bucket, FieldMapping)}.
     */
    public BucketQuery(Bucket bucket, List<String> fields) {
        this(bucket, FieldMapping.select(Objects.requireNonNull(fields, "fields")));
    }

    /** Varargs form of {@link #BucketQuery(Bucket, List)}. */
    public BucketQuery(Bucket bucket, String... fields) {
        this(bucket, FieldMapping.select(Objects.requireNonNull(fields, "fields")));
    }

    /** Load HEAD records only into a table named after the bucket id. */
    public Table table() {
        return table(bucket.config().getBucketId(), 0);
    }

    /**
     * Load records including history.
     *
     * @param historyThrough {@code 0}=HEAD only; {@code N}=HEAD+v1..vN; {@code <0}=HEAD+all versions
     */
    public Table table(int historyThrough) {
        return table(bucket.config().getBucketId(), historyThrough);
    }

    public Table table(String name) {
        return table(name, 0);
    }

    public Table table(String name, int historyThrough) {
        Objects.requireNonNull(name, "name");
        boolean withHistory = historyThrough != 0;
        List<String> ids = bucket.listRecordIds().toList();
        List<Map<String, JsonNode>> rows = new ArrayList<>();
        List<String> columnOrder = mapping != null
                ? new ArrayList<>(mapping.asMap().values())
                : new ArrayList<>();
        if (withHistory) {
            columnOrder.add(0, "version");
        }

        for (String id : ids) {
            List<Integer> versions = bucket.listVersions(id);
            if (versions.isEmpty()) {
                continue;
            }
            int headVersion = versions.get(versions.size() - 1);

            JsonNode head = bucket.readRecord(id).orElse(null);
            if (head != null) {
                addProjectedRow(rows, columnOrder, head, withHistory ? headVersion : null);
            }

            if (!withHistory) {
                continue;
            }

            int maxHistorical = historyThrough < 0
                    ? headVersion
                    : Math.min(historyThrough, headVersion);
            for (int version = 1; version <= maxHistorical; version++) {
                if (version == headVersion || !versions.contains(version)) {
                    continue;
                }
                int historicalVersion = version;
                bucket.readVersionRecord(id, historicalVersion).ifPresent(record ->
                        addProjectedRow(rows, columnOrder, record, historicalVersion));
            }
        }

        return TableBuilder.build(name, columnOrder, rows);
    }

    private void addProjectedRow(
            List<Map<String, JsonNode>> rows,
            List<String> columnOrder,
            JsonNode record,
            Integer version) {
        Map<String, JsonNode> row = project(record);
        if (version != null) {
            Map<String, JsonNode> withVersion = new LinkedHashMap<>();
            withVersion.put("version", IntNode.valueOf(version));
            withVersion.putAll(row);
            row = withVersion;
        }
        if (mapping == null) {
            for (String column : row.keySet()) {
                if (!columnOrder.contains(column)) {
                    columnOrder.add(column);
                }
            }
        }
        rows.add(row);
    }

    private Map<String, JsonNode> project(JsonNode record) {
        if (mapping != null) {
            Map<String, JsonNode> row = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) {
                JsonNode value = JsonPaths.at(record, entry.getKey());
                if (value != null && !value.isMissingNode() && !value.isNull()) {
                    row.put(entry.getValue(), value);
                } else {
                    row.put(entry.getValue(), null);
                }
            }
            return row;
        }

        return JsonPaths.flattenLeaves(record);
    }
}
