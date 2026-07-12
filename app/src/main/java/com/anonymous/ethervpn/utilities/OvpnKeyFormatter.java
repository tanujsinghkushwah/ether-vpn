package com.anonymous.ethervpn.utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for converting an RTDB ovpn key (e.g. "usa-1", "uk-2", "germany-1")
 * into a user-facing display name.
 *
 * <p>Keys follow the pattern {@code <country-slug>-<index>}, e.g. "usa-1", "canada-2".
 * The slug is matched by prefix so that any future index suffix is handled automatically.
 *
 * <p>Unknown slugs gracefully fall back to title-case capitalisation, so a new country added
 */
public final class OvpnKeyFormatter {

    private OvpnKeyFormatter() {}

    // Maps each recognised lowercase slug prefix → canonical display name.
    private static final Map<String, String> SLUG_TO_NAME = new HashMap<>();

    static {
        SLUG_TO_NAME.put("usa",         "United States");
        SLUG_TO_NAME.put("us",          "United States");
        SLUG_TO_NAME.put("uk",          "United Kingdom");
        SLUG_TO_NAME.put("gb",          "United Kingdom");
        SLUG_TO_NAME.put("canada",      "Canada");
        SLUG_TO_NAME.put("ca",          "Canada");
        SLUG_TO_NAME.put("france",      "France");
        SLUG_TO_NAME.put("fr",          "France");
        SLUG_TO_NAME.put("germany",     "Germany");
        SLUG_TO_NAME.put("de",          "Germany");
        SLUG_TO_NAME.put("japan",       "Japan");
        SLUG_TO_NAME.put("jp",          "Japan");
        SLUG_TO_NAME.put("netherlands", "Netherlands");
        SLUG_TO_NAME.put("nl",          "Netherlands");
        SLUG_TO_NAME.put("singapore",   "Singapore");
        SLUG_TO_NAME.put("sg",          "Singapore");
        SLUG_TO_NAME.put("sweden",      "Sweden");
        SLUG_TO_NAME.put("se",          "Sweden");
        SLUG_TO_NAME.put("switzerland", "Switzerland");
        SLUG_TO_NAME.put("ch",          "Switzerland");
        SLUG_TO_NAME.put("australia",   "Australia");
        SLUG_TO_NAME.put("au",          "Australia");
        SLUG_TO_NAME.put("brazil",      "Brazil");
        SLUG_TO_NAME.put("br",          "Brazil");
    }

    /**
     * Converts a raw RTDB key to a display name.
     *
     * <pre>
     *   displayName("usa-1")     → "United States"
     *   displayName("uk-2")      → "United Kingdom"
     *   displayName("germany-1") → "Germany"
     *   displayName("newplace-1")→ "Newplace"   (graceful fallback)
     * </pre>
     */
    public static String displayName(String key) {
        if (key == null || key.isEmpty()) return "";
        String slug = slugOnly(key);
        String name = SLUG_TO_NAME.get(slug);
        if (name != null) return name;
        // Graceful fallback: title-case the slug.
        return slug.isEmpty() ? key
                : Character.toUpperCase(slug.charAt(0)) + slug.substring(1);
    }

    /**
     * Strips the trailing numeric index from an RTDB key, returning just the slug.
     *
     * <pre>
     *   slugOnly("usa-1")  → "usa"
     *   slugOnly("uk-2")   → "uk"
     *   slugOnly("usa")    → "usa"
     * </pre>
     */
    public static String slugOnly(String key) {
        if (key == null) return "";
        int dash = key.lastIndexOf('-');
        if (dash > 0 && dash < key.length() - 1) {
            String suffix = key.substring(dash + 1);
            if (suffix.matches("\\d+")) return key.substring(0, dash).toLowerCase();
        }
        return key.toLowerCase();
    }
}
