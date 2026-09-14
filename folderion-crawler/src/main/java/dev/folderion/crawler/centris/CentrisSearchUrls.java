package dev.folderion.crawler.centris;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for Centris search URLs ({@code page} / {@code pageSize} query params).
 *
 * <p>Mutates only the target query key in the <em>raw</em> query string so encoded values
 * like {@code q=} (base64url) are preserved byte-for-byte.
 */
public final class CentrisSearchUrls {

    private static final Pattern PAGE = Pattern.compile("(?:^|&)page=([^&]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_SIZE = Pattern.compile("(?:^|&)pagesize=([^&]*)", Pattern.CASE_INSENSITIVE);

    private CentrisSearchUrls() {
    }

    public static int page(String searchUrl) {
        return Math.max(1, intParam(searchUrl, PAGE, 1));
    }

    public static int pageSize(String searchUrl) {
        return Math.max(1, intParam(searchUrl, PAGE_SIZE, 20));
    }

    public static String withPage(String searchUrl, int page) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        return withRawQueryParam(searchUrl, "page", String.valueOf(page));
    }

    static String withRawQueryParam(String searchUrl, String name, String value) {
        Objects.requireNonNull(searchUrl, "searchUrl");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");

        URI uri = URI.create(searchUrl);
        String rawQuery = uri.getRawQuery();
        String key = name.toLowerCase(Locale.ROOT);

        String newQuery;
        if (rawQuery == null || rawQuery.isBlank()) {
            newQuery = key + "=" + value;
        } else if (containsParam(rawQuery, key)) {
            newQuery = replaceParam(rawQuery, key, value);
        } else {
            newQuery = rawQuery + "&" + key + "=" + value;
        }

        return rebuild(uri, newQuery);
    }

    private static boolean containsParam(String rawQuery, String key) {
        for (String part : rawQuery.split("&", -1)) {
            int eq = part.indexOf('=');
            String partKey = eq >= 0 ? part.substring(0, eq) : part;
            if (partKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static String replaceParam(String rawQuery, String key, String value) {
        String[] parts = rawQuery.split("&", -1);
        StringBuilder out = new StringBuilder();
        boolean replaced = false;
        for (String part : parts) {
            if (part.isEmpty() && out.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String partKey = eq >= 0 ? part.substring(0, eq) : part;
            String next;
            if (partKey.equalsIgnoreCase(key)) {
                next = key + "=" + value;
                replaced = true;
            } else {
                next = part;
            }
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(next);
        }
        if (!replaced) {
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(key).append('=').append(value);
        }
        return out.toString();
    }

    private static String rebuild(URI uri, String rawQuery) {
        StringBuilder out = new StringBuilder();
        if (uri.getScheme() != null) {
            out.append(uri.getScheme()).append("://");
        }
        if (uri.getRawAuthority() != null) {
            out.append(uri.getRawAuthority());
        }
        out.append(uri.getRawPath() != null ? uri.getRawPath() : "");
        if (rawQuery != null && !rawQuery.isBlank()) {
            out.append('?').append(rawQuery);
        }
        if (uri.getRawFragment() != null) {
            out.append('#').append(uri.getRawFragment());
        }
        return out.toString();
    }

    private static int intParam(String searchUrl, Pattern pattern, int defaultValue) {
        String rawQuery = URI.create(searchUrl).getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return defaultValue;
        }
        Matcher matcher = pattern.matcher(rawQuery);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
