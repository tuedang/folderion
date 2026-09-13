package dev.folderion.query;

import com.fasterxml.jackson.databind.JsonNode;
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
 * <p>{@link FieldMapping} is optional: when provided, only mapped paths become columns (renamed);
 * when omitted, leaf JSON fields are auto-flattened using dotted paths as column names.
 *
 * <pre>{@code
 * FieldMapping mapping = FieldMapping.of(
 *         "title", "title",
 *         "price.amount", "price",
 *         "address.city", "city",
 *         "features.yearBuilt", "year");
 * Table table = new BucketQuery(bucket, mapping).table();
 * table.print();
 * }</pre>
 */
public final class BucketQuery {

    private final Bucket bucket;
    private final FieldMapping mapping;

    public BucketQuery(Bucket bucket) {
        this(bucket, null);
    }

    public BucketQuery(Bucket bucket, FieldMapping mapping) {
        this.bucket = Objects.requireNonNull(bucket, "bucket");
        this.mapping = mapping == null || mapping.isEmpty() ? null : mapping;
    }

    /** Load all HEAD records into a table named after the bucket id. */
    public Table table() {
        return table(bucket.config().getBucketId());
    }

    public Table table(String name) {
        Objects.requireNonNull(name, "name");
        List<String> ids = bucket.listRecordIds().toList();
        List<Map<String, JsonNode>> rows = new ArrayList<>(ids.size());
        List<String> columnOrder = mapping != null
                ? new ArrayList<>(mapping.asMap().values())
                : new ArrayList<>();

        for (String id : ids) {
            JsonNode record = bucket.readRecord(id).orElse(null);
            if (record == null) {
                continue;
            }
            Map<String, JsonNode> row = project(record);
            if (mapping == null) {
                for (String column : row.keySet()) {
                    if (!columnOrder.contains(column)) {
                        columnOrder.add(column);
                    }
                }
            }
            rows.add(row);
        }

        return TableBuilder.build(name, columnOrder, rows);
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
