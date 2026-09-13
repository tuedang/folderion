package dev.folderion.query;

import com.fasterxml.jackson.databind.JsonNode;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a Tablesaw {@link Table} from projected row maps (column → cell).
 */
final class TableBuilder {

    private enum Kind {
        BOOLEAN,
        LONG,
        DOUBLE,
        STRING
    }

    private TableBuilder() {
    }

    static Table build(String name, List<String> columnOrder, List<Map<String, JsonNode>> rows) {
        Set<String> columns = new LinkedHashSet<>(columnOrder);
        for (Map<String, JsonNode> row : rows) {
            columns.addAll(row.keySet());
        }
        List<String> ordered = new ArrayList<>(columns);
        Map<String, Kind> types = inferTypes(ordered, rows);

        Map<String, Column<?>> built = new LinkedHashMap<>();
        for (String column : ordered) {
            built.put(column, emptyColumn(column, types.get(column)));
        }

        for (Map<String, JsonNode> row : rows) {
            for (String column : ordered) {
                append(built.get(column), types.get(column), row.get(column));
            }
        }

        return Table.create(name, built.values());
    }

    private static Map<String, Kind> inferTypes(List<String> columns, List<Map<String, JsonNode>> rows) {
        Map<String, Kind> types = new LinkedHashMap<>();
        for (String column : columns) {
            types.put(column, inferType(column, rows));
        }
        return types;
    }

    private static Kind inferType(String column, List<Map<String, JsonNode>> rows) {
        boolean sawBoolean = false;
        boolean sawIntegral = false;
        boolean sawFloating = false;
        boolean sawOther = false;

        for (Map<String, JsonNode> row : rows) {
            JsonNode value = row.get(column);
            if (value == null || value.isNull() || value.isMissingNode()) {
                continue;
            }
            if (value.isBoolean()) {
                sawBoolean = true;
            } else if (value.isIntegralNumber()) {
                sawIntegral = true;
            } else if (value.isFloatingPointNumber() || value.isNumber()) {
                sawFloating = true;
            } else {
                sawOther = true;
            }
        }

        if (sawOther || (sawBoolean && (sawIntegral || sawFloating))) {
            return Kind.STRING;
        }
        if (sawBoolean) {
            return Kind.BOOLEAN;
        }
        if (sawFloating) {
            return Kind.DOUBLE;
        }
        if (sawIntegral) {
            return Kind.LONG;
        }
        return Kind.STRING;
    }

    private static Column<?> emptyColumn(String name, Kind type) {
        return switch (type) {
            case BOOLEAN -> BooleanColumn.create(name);
            case LONG -> LongColumn.create(name);
            case DOUBLE -> DoubleColumn.create(name);
            case STRING -> StringColumn.create(name);
        };
    }

    private static void append(Column<?> column, Kind type, JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            column.appendMissing();
            return;
        }
        switch (type) {
            case BOOLEAN -> ((BooleanColumn) column).append(value.asBoolean());
            case LONG -> ((LongColumn) column).append(value.longValue());
            case DOUBLE -> ((DoubleColumn) column).append(value.asDouble());
            case STRING -> ((StringColumn) column).append(JsonPaths.asCellText(value));
        }
    }
}
