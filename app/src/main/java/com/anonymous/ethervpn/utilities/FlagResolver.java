package com.anonymous.ethervpn.utilities;

import com.anonymous.ethervpn.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps a server country name (as it comes from Firebase remote-config) to a bundled
 * vector flag drawable id. Returns 0 when no match is found so callers can fall back
 * to a Glide-loaded URL.
 */
public class FlagResolver {

    private static final Map<String, Integer> MAP = new HashMap<>();

    static {
        // Full country names
        MAP.put("United Kingdom", R.drawable.flag_uk);
        MAP.put("United States", R.drawable.flag_us);
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
        // ISO-2 codes
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
        // Common lowercase variants
        MAP.put("uk", R.drawable.flag_uk);
        MAP.put("us", R.drawable.flag_us);
        MAP.put("usa", R.drawable.flag_us);
        MAP.put("de", R.drawable.flag_de);
        MAP.put("fr", R.drawable.flag_fr);
        MAP.put("jp", R.drawable.flag_jp);
        MAP.put("nl", R.drawable.flag_nl);
        MAP.put("sg", R.drawable.flag_sg);
        MAP.put("ca", R.drawable.flag_ca);
        MAP.put("se", R.drawable.flag_se);
        MAP.put("ch", R.drawable.flag_ch);
        MAP.put("au", R.drawable.flag_au);
        MAP.put("br", R.drawable.flag_br);
        // Full lowercase country names (matches raw OVPN keys before display formatting)
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

    /** @return drawable resource id, or 0 if no match. */
    public static int resolve(String countryNameOrCode) {
        if (countryNameOrCode == null) return 0;
        String name = countryNameOrCode.trim();
        Integer id = MAP.get(name);
        if (id != null) return id;

        // Strip trailing numeric suffix (e.g. "United States-1" → "United States",
        // "germany-1" → "germany"). Disambiguation appends -N to duplicates.
        String stripped = stripNumberSuffix(name);
        if (!stripped.equals(name)) {
            id = MAP.get(stripped);
            if (id != null) return id;
        }

        // Try ISO-2 from the first two letters
        if (stripped.length() >= 2) {
            id = MAP.get(stripped.substring(0, 2).toUpperCase());
            if (id != null) return id;
        }

        // Try lowercase form (handles raw keys like "germany", "japan")
        id = MAP.get(stripped.toLowerCase());
        if (id != null) return id;

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
