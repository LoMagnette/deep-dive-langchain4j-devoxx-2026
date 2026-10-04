package dev.devoxx.dashboard.support;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Defensive readers for what a model actually returns, as opposed to what it was asked for.
 */
public final class Parsing {

    private Parsing() {
    }

    private static final String N = "\\d+(?:[.,]\\d+)?";
    private static final Pattern NUMBER = Pattern.compile(N);

    /**
     * "3/4", "3 of 4", "8.5 out of 10" — a fraction stated as two numbers, which is the form the
     * critic's own prompt asks for ("the fraction of rules that hold"). Tried first, because the
     * numerator on its own is not the score: reading only the first number turns "4 of 4 rules
     * hold" into 0.4 and a perfect note never leaves the loop.
     */
    private static final Pattern FRACTION =
            Pattern.compile("(" + N + ")\\s*(?:/|out\\s+of|of)\\s*(" + N + ")",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Reads the loop's quality score defensively, in the order a reader would.
     */
    public static double score(String answer) {
        // Emphasis first: a model writes "**0.85** out of 1.0", and the asterisks sit exactly
        // between the two halves of the fraction, so the fraction never matches and the scale
        // (1.0) is read as the score.
        String text = (answer == null ? "" : answer).replaceAll("[*_`]", "");

        var f = FRACTION.matcher(text);
        if (f.find()) {
            double over = num(f.group(2));
            if (over > 0) {
                return clamp(num(f.group(1)) / over);
            }
        }

        var m = NUMBER.matcher(text);
        Double lastAny = null;
        Double lastInRange = null;
        while (m.find()) {
            double v = num(m.group());
            lastAny = v;
            if (v <= 1.0) {
                lastInRange = v;
            }
        }
        if (lastInRange != null) {
            return lastInRange;
        }
        if (lastAny == null) {
            return 0.0;
        }
        double v = lastAny;
        while (v > 1.0) {
            v /= 10.0;
        }
        return v;
    }

    private static double num(String matched) {
        return Double.parseDouble(matched.replace(',', '.'));
    }

    private static double clamp(double v) {
        return v < 0 ? 0 : Math.min(v, 1.0);
    }

    /**
     * The four Rangers the emergency phone can ring, by what the call is about. {@code hurt} is
     * FIRST because it is the fallback: when Zao cannot tell what a call is, the tolerable mistake
     * is sending Doc, not sending Zoom to a pup who is bleeding.
     */
    private static final List<String> CATEGORIES = List.of("hurt", "lost", "underground", "urgent");

    /**
     * Normalises the classifier's answer to exactly one known destination. Asked to "return one
     * word", a real model answers "This is best categorised as: **lost**." — an exact
     * {@code equalsIgnoreCase} then matches no branch at all and the run silently produces null.
     * We take the LAST one mentioned (models state the conclusion at the end) and fall back to
     * the first, which is deliberately {@code hurt}.
     */
    public static String category(String answer) {
        String raw = (answer == null ? "" : answer).toLowerCase(Locale.ROOT);
        String best = CATEGORIES.get(0);
        int bestAt = -1;
        for (String c : CATEGORIES) {
            int at = raw.lastIndexOf(c);
            if (at > bestAt) {
                bestAt = at;
                best = c;
            }
        }
        return best;
    }

    private static final Pattern LABELLED_SCORE =
            Pattern.compile("score\\s*[:=]\\s*(" + N + "(?:\\s*(?:/|out\\s+of|of)\\s*" + N + ")?)",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Fifi's review is "SCORE: 0.6 / FEEDBACK: the date is missing …", and the feedback is prose
     * that can hold any number ("1 typo", "2 o'clock"). So the number after the SCORE label wins,
     * and only a review with no label at all falls back to {@link #score}.
     */
    public static double reviewScore(String review) {
        String text = (review == null ? "" : review).replaceAll("[*_`]", "");
        var m = LABELLED_SCORE.matcher(text);
        return m.find() ? score(m.group(1)) : score(text);
    }

    /**
     * Whether a peer has found what the maze was searched for. The CONTENT is read, never the
     * presence of a key: a predicate asking whether a peer's own output key exists is true the
     * moment that peer has spoken, and ends a search that has not found anything.
     */
    public static boolean found(String report) {
        if (report == null) {
            return false;
        }
        String t = report.toLowerCase(Locale.ROOT);
        int at = t.indexOf("found:");
        return at >= 0 && !t.substring(Math.max(0, at - 4), at).contains("not");
    }

    /**
     * The first number in a sentence — "the oak is 6 m up to the branch" — for the plain-Java
     * steps that need a measurement. Deciding what was measured is the Ranger's job; reading the
     * digits back out is ours.
     */
    public static double firstNumber(String text, double fallback) {
        var m = NUMBER.matcher(text == null ? "" : text);
        return m.find() ? num(m.group()) : fallback;
    }

    /** The eight ducklings, for when the input box holds nothing to fan out over. */
    private static final List<String> DUCKLINGS = List.of(
            "Puddle — last seen at the duck pond", "Pickle — last seen by the bakery bins",
            "Waddles — last seen on the town hall steps", "Biscuit — last seen in the fountain",
            "Noodle — last seen under the bandstand", "Pip — last seen at the bus stop",
            "Socks — last seen in the Mayor's roses", "Bean — last seen following Marmalade");

    /**
     * Splits the user's typed input into items for the parallel mapper.
     */
    public static List<String> items(String input) {
        String text = input == null ? "" : input;
        if (text.isBlank()) {
            return DUCKLINGS;
        }
        // Semicolons and newlines beat commas when both are present: an item can contain a
        // comma, and splitting on everything fans the mapper out over half-items.
        String separators = text.matches("(?s).*[;\n].*") ? "[;\n]" : ",";
        List<String> parsed = Arrays.stream(text.split(separators))
                .map(String::trim)
                // Something has to be IN the chunk: an input of separators and spaces otherwise
                // fans the mapper out over a stray comma and asks an agent whether it is safe.
                .filter(s -> s.matches("(?s).*[\\p{L}\\p{N}].*"))
                .toList();
        // Only when the split produced nothing at all (an input of separators and spaces).
        return parsed.isEmpty() ? DUCKLINGS : parsed;
    }
}
