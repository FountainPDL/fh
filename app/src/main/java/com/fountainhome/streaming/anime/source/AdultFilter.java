package com.fountainhome.streaming.anime.source;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Safety net for the 18+ sites: anything tagged or titled as depicting minors is never listed, opened or searched.
 * Same word lists as the original app; whole-word for the short ones ("child bearing hips" is fine, "loli" is not).
 */
public final class AdultFilter {
    private AdultFilter() {}

    private static final Set<String> WORDS = new HashSet<>(Arrays.asList(
        "loli", "lolicon", "shota", "shotacon", "toddlercon", "underage", "preteen", "infant", "cub"));
    private static final Set<String> TAGS = new HashSet<>(Arrays.asList(
        "child", "children", "kid", "kids", "toddler", "minor", "young child"));
    private static final String[] PREFIXES = {"loli", "shota", "shouta", "toddlercon", "underage", "preteen"};

    /** true = hide it. */
    public static boolean blocked(List<String> values) {
        for (String raw : values) {
            if (raw == null) continue;
            String tag = raw.replaceAll("[^A-Za-z0-9_]+", " ").toLowerCase(Locale.ROOT).replace('_', ' ').trim();
            if (TAGS.contains(tag)) return true;
            for (String w : tag.split(" ")) {
                if (WORDS.contains(w)) return true;
                for (String p : PREFIXES) if (w.startsWith(p)) return true;
            }
        }
        return false;
    }

    public static boolean blocked(String... values) { return blocked(Arrays.asList(values)); }
}
