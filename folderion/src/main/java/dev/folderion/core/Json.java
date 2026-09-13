package dev.folderion.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private Json() {
    }

    static ObjectMapper mapper() {
        return MAPPER;
    }

    static <T> T read(Path path, Class<T> type) {
        try (InputStream in = Files.newInputStream(path)) {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new FolderionException("Failed to read JSON: " + path, e);
        }
    }

    static <T> T read(InputStream in, Class<T> type) {
        try {
            return MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new FolderionException("Failed to read JSON stream", e);
        }
    }

    static JsonNode readTree(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new FolderionException("Failed to read JSON: " + path, e);
        }
    }

    static JsonNode readTree(InputStream in) {
        try {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new FolderionException("Failed to read JSON stream", e);
        }
    }

    static JsonNode valueToTree(Object value) {
        return MAPPER.valueToTree(value);
    }

    static void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(path)) {
                MAPPER.writerWithDefaultPrettyPrinter().writeValue(out, value);
            }
        } catch (IOException e) {
            throw new FolderionException("Failed to write JSON: " + path, e);
        }
    }

    static String writeCanonical(JsonNode node) {
        try {
            ObjectMapper canonical = MAPPER.copy()
                    .configure(SerializationFeature.INDENT_OUTPUT, false)
                    .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return canonical.writeValueAsString(sortTree(node));
        } catch (IOException e) {
            throw new FolderionException("Failed to canonicalize JSON", e);
        }
    }

    private static JsonNode sortTree(JsonNode node) {
        if (node == null || !node.isObject()) {
            return node;
        }
        ObjectNode sorted = MAPPER.createObjectNode();
        node.properties().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach(e -> sorted.set(e.getKey(), sortTree(e.getValue())));
        return sorted;
    }
}
