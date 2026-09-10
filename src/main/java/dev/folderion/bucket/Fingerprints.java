package dev.folderion.bucket;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Content fingerprinting for change detection (version gate).
 */
public final class Fingerprints {

    private Fingerprints() {
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new FolderionException("SHA-256 not available", e);
        }
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Fingerprint business payload: either explicit fields or entire record minus runtime keys.
     * Field names may be top-level ({@code price}) or dotted into objects ({@code price.amount}).
     * Array element paths (e.g. {@code open_houses.0.date}) are not supported — use the parent
     * field ({@code open_houses}) to fingerprint the whole array.
     */
    public static String contentFingerprint(JsonNode record, List<String> fields) {
        JsonNode payload;
        if (fields == null || fields.isEmpty()) {
            payload = stripRuntime(record.deepCopy());
        } else {
            var object = Json.mapper().createObjectNode();
            for (String field : fields) {
                JsonNode value = atPath(record, field);
                if (value != null) {
                    object.set(field, value);
                }
            }
            payload = object;
        }
        return sha256Hex(Json.writeCanonical(payload));
    }

    static JsonNode atPath(JsonNode root, String path) {
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

    public static String mediaFingerprint(List<String> blobDigestsInOrder) {
        String joined = String.join("\n", blobDigestsInOrder);
        return sha256Hex(joined);
    }

    private static JsonNode stripRuntime(JsonNode node) {
        if (node != null && node.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("crawl");
            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("integrity");
        }
        return node;
    }
}
