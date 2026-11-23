package io.github.bryceaspringfield.morphe.extension.reddit;

import java.lang.reflect.Method;

/**
 * The rules of Morphe's own Reddit filters (Keyword filters and Flair filters, morphe-patches 2.4.0 on), when that bundle is
 * applied too, in this filter's syntax: each keyword filter, a regular expression, as "re:" plus it, and each flair filter
 * ("community::flair") as "r/community: flair:flair". This filter then applies its own reach to them: comments, search,
 * profiles and the rest. Empty when the mainline patches are not in the app.
 */
final class Mainline {
    private Mainline() {
    }

    // read again at most every two seconds: rule lookups run for every post in a feed
    private static volatile long readAt;
    private static volatile String keywords = "", flairs = "";

    private static void refresh() {
        long now = System.currentTimeMillis();
        if (now - readAt < 2000) return;
        readAt = now;
        keywords = setting("KEYWORD_FILTERS");
        flairs = setting("FLAIR_FILTERS");
    }

    private static String setting(String name) {
        try {
            Class<?> settings = Class.forName("app.morphe.extension.reddit.settings.Settings");
            Object setting = settings.getField(name).get(null);
            Method get = setting.getClass().getMethod("get");
            Object value = get.invoke(setting);
            return value == null ? "" : value.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** The mainline keyword filters as rule lines. */
    static String keywordRules() {
        StringBuilder out = new StringBuilder();
        refresh();
        for (String line : keywords.split("\n")) {
            line = line.trim();
            if (!line.isEmpty()) out.append("\nre:").append(line);
        }
        return out.toString();
    }

    /** The mainline flair filters as rule lines. */
    static String flairRules() {
        StringBuilder out = new StringBuilder();
        refresh();
        for (String line : flairs.split("\n")) {
            int at = line.indexOf("::");
            if (at <= 0) continue;
            String community = line.substring(0, at).trim();
            if (community.regionMatches(true, 0, "r/", 0, 2)) community = community.substring(2);
            String flair = line.substring(at + 2).trim();
            if (!community.isEmpty() && !flair.isEmpty()) out.append("\nr/").append(community).append(": flair:\"").append(flair).append('"');
        }
        return out.toString();
    }
}
