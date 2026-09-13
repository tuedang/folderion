package dev.folderion.query;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dotted-path helpers over Jackson {@link JsonNode} trees (for tabular projection).
 */
final class JsonPaths {

    private static final Set<String> SKIP_ROOT_KEYS = Set.of("crawl", "integrity");

    private JsonPaths() {
    }

    static JsonNode at(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String part : path.split("\\.")) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }
        return current;
    }

    /**
     * Flatten object leaves to dotted paths. Nested objects are expanded; arrays and value nodes
     * become leaves. Runtime keys {@code crawl} / {@code integrity} are skipped at the root.
     */
    static Map<String, JsonNode> flattenLeaves(JsonNode root) {
        Map<String, JsonNode> out = new LinkedHashMap<>();
        if (root == null || root.isNull() || root.isMissingNode()) {
            return out;
        }
        walk(root, "", out, true);
        return out;
    }

    private static void walk(JsonNode node, String prefix, Map<String, JsonNode> out, boolean atRoot) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isValueNode() || node.isArray()) {
            if (!prefix.isEmpty()) {
                out.put(prefix, node);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        node.properties().forEach(entry -> {
            String key = entry.getKey();
            if (atRoot && SKIP_ROOT_KEYS.contains(key)) {
                return;
            }
            String next = prefix.isEmpty() ? key : prefix + "." + key;
            walk(entry.getValue(), next, out, false);
        });
    }

    static String asCellText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return node.toString();
    }
}
