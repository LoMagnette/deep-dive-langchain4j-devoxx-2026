package dev.devoxx.dashboard.support;

import static dev.devoxx.dashboard.support.Wiring.str;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Defensive readers for what a model actually returns, as opposed to what it was asked for.
 * Every method here exists because a real model broke a pattern in a way that produced no
 * error at all — a score as prose, a category wrapped in a sentence, a list that wasn't one.
 */
public final class Parsing {

    private Parsing() {
    }

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");

    /**
     * Reads the loop's quality score defensively. Asked for "just a number", a real model
     * cheerfully answers "I'd rate this **8.5** out of 10" — so pull out the first number and
     * rescale anything above 1.0. An unparseable answer scores 0, which keeps the loop iterating
     * rather than exiting on garbage.
     */
    public static double score(AgenticScope s) {
        var m = NUMBER.matcher(str(s, "score"));
        if (!m.find()) {
            return 0.0;
        }
        double v = Double.parseDouble(m.group().replace(',', '.'));
        while (v > 1.0) {
            v /= 10.0;
        }
        return v;
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
    public static String category(AgenticScope s) {
        String raw = str(s, "category").toLowerCase(Locale.ROOT);
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

    /**
     * Splits the user's typed input into items for the parallel mapper. A single-chunk input
     * (nothing to fan out over) falls back to a canned picnic blanket.
     */
    public static List<String> items(String input) {
        String text = String.valueOf(input);
        // Semicolons and newlines win over commas when both are present, because an item can
        // itself contain a comma ("a bar of dark chocolate, most of it"). Splitting on every
        // separator at once fans the mapper out over shrapnel — with no error at all, just a
        // list of answers to half-items.
        String separators = text.matches("(?s).*[;\n].*") ? "[;\n]" : ",";
        List<String> parsed = Arrays.stream(text.split(separators))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return parsed.size() > 1 ? parsed
                : List.of("a handful of grapes", "a slice of cheddar",
                        "a square of dark chocolate", "a crust of bread", "half a raw onion");
    }
}
