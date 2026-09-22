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

    /** The three people the owner's worry can be sent to. */
    private static final List<String> CATEGORIES = List.of("emergency", "training", "everyday");

    /**
     * Normalises the router's answer to exactly one known destination. Asked to "return one
     * word", a real model answers "This is best categorised as: **medical**." — an exact
     * {@code equalsIgnoreCase} then matches no branch at all and the run silently produces null.
     * We take the LAST one mentioned (models state the conclusion at the end) and fall back to
     * the first, which is deliberately {@code emergency}: when the classifier is unsure about a
     * dog, the tolerable mistake is bothering the vet, not routing a poisoning to the trainer.
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

    /** What the blanket had on it, for when there is genuinely nothing to fan out over. */
    private static final List<String> BLANKET =
            List.of("a handful of grapes", "a slice of cheddar", "a square of dark chocolate",
                    "a crust of bread", "half a raw onion");

    /**
     * Splits the user's typed input into items for the parallel mapper.
     */
    public static List<String> items(String input) {
        String text = input == null ? "" : input;
        if (text.isBlank()) {
            return BLANKET;
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
        return parsed.isEmpty() ? BLANKET : parsed;
    }
}
