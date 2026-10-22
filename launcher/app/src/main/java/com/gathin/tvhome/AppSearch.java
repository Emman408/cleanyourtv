package com.gathin.tvhome;

import java.text.Normalizer;
import java.util.Locale;

/** Local matching: display names only; no app discovery, persistence or UI mutation. */
final class AppSearch {
    private AppSearch() { }

    /** Build once when labels load, using the activity's current resource locale. Never change display text. */
    static String key(String text, Locale locale) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text.toLowerCase(locale), Normalizer.Form.NFD)
                .replace("ß", "ss").replace('ς', 'σ');
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < decomposed.length();) {
            int c = decomposed.codePointAt(i);
            i += Character.charCount(c);
            int type = Character.getType(c);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK) continue;
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                space = out.length() > 0;
            } else {
                if (space) out.append(' ');
                out.appendCodePoint(c);
                space = false;
            }
        }
        return out.toString();
    }

    /** Normalize the query once, then check every word against each precomputed display-name key. */
    static String[] words(String query, Locale locale) {
        String normalized = key(query, locale);
        return normalized.isEmpty() ? new String[0] : normalized.split(" ");
    }

    static boolean matches(String labelKey, String[] words) {
        for (String word : words) if (!labelKey.contains(word)) return false;
        return true;
    }

    /** Follow the selected package when present; otherwise keep a nearby position, or none for an empty list. */
    static int focusIndex(int found, int previous, int count) {
        if (count == 0) return -1;
        return found >= 0 ? found : Math.max(0, Math.min(previous, count - 1));
    }
}
