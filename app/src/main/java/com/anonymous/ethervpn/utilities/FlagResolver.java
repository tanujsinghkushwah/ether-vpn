package com.anonymous.ethervpn.utilities;

import com.anonymous.ethervpn.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a server country name or RTDB key to a bundled vector flag drawable id.
 * Returns 0 when no match is found so callers can fall back to a Glide-loaded URL.
 *
 * <p>Two entry points:
 * <ul>
 *   <li>{@link #resolve(String)} — accepts display names ("United States"), ISO-2 codes
 *       ("US"), or lowercase slugs ("usa"). Backwards-compatible with all existing callers.</li>
 *   <li>{@link #resolveKey(String)} — accepts a raw RTDB ovpn key ("usa-1", "uk-2").
 *       Preferred for new call-sites: never depends on the display name string, so it is
 *       immune to disambiguated suffixes like "United States-2".</li>
 * </ul>
 */
public class FlagResolver {

    private static final Map<String, Integer> MAP = new HashMap<>();

    static {
        // Full country names (title-case, as stored in Server.country)
        MAP.put("United Kingdom", R.drawable.flag_uk);
        MAP.put("United States",  R.drawable.flag_us);
        MAP.put("Germany",        R.drawable.flag_de);
        MAP.put("France",         R.drawable.flag_fr);
        MAP.put("Japan",          R.drawable.flag_jp);
        MAP.put("Netherlands",    R.drawable.flag_nl);
        MAP.put("Singapore",      R.drawable.flag_sg);
        MAP.put("Canada",         R.drawable.flag_ca);
        MAP.put("Sweden",         R.drawable.flag_se);
        MAP.put("Switzerland",    R.drawable.flag_ch);
        MAP.put("Australia",      R.drawable.flag_au);
        MAP.put("Brazil",         R.drawable.flag_br);
        // ISO-2 codes (upper-case)
        MAP.put("UK", R.drawable.flag_uk);
        MAP.put("GB", R.drawable.flag_uk);
        MAP.put("US", R.drawable.flag_us);
        MAP.put("DE", R.drawable.flag_de);
        MAP.put("FR", R.drawable.flag_fr);
        MAP.put("JP", R.drawable.flag_jp);
        MAP.put("NL", R.drawable.flag_nl);
        MAP.put("SG", R.drawable.flag_sg);
        MAP.put("CA", R.drawable.flag_ca);
        MAP.put("SE", R.drawable.flag_se);
        MAP.put("CH", R.drawable.flag_ch);
        MAP.put("AU", R.drawable.flag_au);
        MAP.put("BR", R.drawable.flag_br);
        // Lowercase slugs — matches OvpnKeyFormatter.slugOnly() output and raw RTDB keys
        MAP.put("uk",          R.drawable.flag_uk);
        MAP.put("gb",          R.drawable.flag_uk);
        MAP.put("us",          R.drawable.flag_us);
        MAP.put("usa",         R.drawable.flag_us);
        MAP.put("de",          R.drawable.flag_de);
        MAP.put("fr",          R.drawable.flag_fr);
        MAP.put("jp",          R.drawable.flag_jp);
        MAP.put("nl",          R.drawable.flag_nl);
        MAP.put("sg",          R.drawable.flag_sg);
        MAP.put("ca",          R.drawable.flag_ca);
        MAP.put("se",          R.drawable.flag_se);
        MAP.put("ch",          R.drawable.flag_ch);
        MAP.put("au",          R.drawable.flag_au);
        MAP.put("br",          R.drawable.flag_br);
        MAP.put("germany",     R.drawable.flag_de);
        MAP.put("france",      R.drawable.flag_fr);
        MAP.put("canada",      R.drawable.flag_ca);
        MAP.put("japan",       R.drawable.flag_jp);
        MAP.put("netherlands", R.drawable.flag_nl);
        MAP.put("singapore",   R.drawable.flag_sg);
        MAP.put("sweden",      R.drawable.flag_se);
        MAP.put("switzerland", R.drawable.flag_ch);
        MAP.put("australia",   R.drawable.flag_au);
        MAP.put("brazil",      R.drawable.flag_br);
    }

    /**
     * Resolves a display name, ISO-2 code, or lowercase slug to a flag drawable id.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Exact match (e.g. "United Kingdom", "US")</li>
     *   <li>Strip trailing numeric suffix, then exact match
     *       (handles disambiguated names like "United States-2")</li>
     *   <li>First-two-letters ISO-2 upper-case lookup</li>
     *   <li>Lower-case slug lookup (e.g. "germany", "usa")</li>
     * </ol>
     *
     * @return drawable resource id, or 0 if no match.
     */
    public static int resolve(String countryNameOrCode) {
        if (countryNameOrCode == null) return 0;
        String name = countryNameOrCode.trim();
        Integer id = MAP.get(name);
        if (id != null) return id;

        // Strip trailing numeric suffix (e.g. "United States-2" -> "United States")
        String stripped = stripNumberSuffix(name);
        if (!stripped.equals(name)) {
            id = MAP.get(stripped);
            if (id != null) return id;
        }

        // Try ISO-2 from the first two letters of the stripped value
        if (stripped.length() >= 2) {
            id = MAP.get(stripped.substring(0, 2).toUpperCase());
            if (id != null) return id;
        }

        // Try lowercase slug form (handles "germany", "usa", etc.)
        id = MAP.get(stripped.toLowerCase());
        if (id != null) return id;

        return 0;
    }

    /**
     * Resolves a raw RTDB ovpn key ("usa-1", "uk-2", "germany-1") to a flag drawable id.
     *
     * <p>This is the preferred entry point for code that works directly with RTDB keys,
     * because it never relies on the display name string and is therefore immune to
     * disambiguated suffixes like "United States-2".
     *
     * @param ovpnKey raw RTDB key or filename (e.g. "uk-2", "usa-1.ovpn")
     * @return drawable resource id, or 0 if no match.
     */
    public static int resolveKey(String ovpnKey) {
        if (ovpnKey == null) return 0;
        String key = ovpnKey.trim().replace(".ovpn", "");
        String slug = OvpnKeyFormatter.slugOnly(key);
        Integer id = MAP.get(slug);
        if (id != null) return id;
        if (slug.length() >= 2) {
            id = MAP.get(slug.substring(0, 2).toUpperCase());
            if (id != null) return id;
        }
        return 0;
    }

    private static String stripNumberSuffix(String s) {
        int dash = s.lastIndexOf('-');
        if (dash > 0 && dash < s.length() - 1) {
            String suffix = s.substring(dash + 1);
            if (suffix.matches("\\d+")) return s.substring(0, dash);
        }
        return s;
    }
}
