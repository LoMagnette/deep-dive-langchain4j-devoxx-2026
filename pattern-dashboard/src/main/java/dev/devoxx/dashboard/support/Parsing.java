package dev.devoxx.dashboard.support;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Defensive readers for what a model actually returns, as opposed to what it was asked for.
 *
 * <p>These take the model's answer as a plain {@code String} rather than reaching into an
 * {@code AgenticScope} themselves. That is deliberate: reading the scope is LangChain4j API and
 * belongs in the demo where the room can see it, so a predicate reads
 * {@code scope.readState(Score.class)} and hands the text here. What is left in this class is
 * only the part that is ours — the parsing no framework can do for you.
 * Every method here exists because a real model broke a pattern in a way that produced no
 * error at all — a score as prose, a category wrapped in a sentence, a list that wasn't one.
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
     *
     * <p>Asked for "just a number from 0.0 to 1.0", a real model answers "3 of 4", "9/10",
     * "I'd rate this **0.85** out of 1.0" or "85%". Three passes, narrowest first:
     * <ol>
     *   <li>a stated <b>fraction</b> — {@code 3/4}, {@code 3 of 4}, {@code 8.5 out of 10};</li>
     *   <li>otherwise the <b>last</b> number that is already in 0.0–1.0, because a model states
     *       its conclusion at the end and any number before it is usually a rule index
     *       ("rule 3 fails, so 0.4");</li>
     *   <li>otherwise the last number at all, rescaled down — which is what turns 85 into 0.85.</li>
     * </ol>
     *
     * <p>An unparseable answer scores 0, which keeps the loop iterating rather than exiting on
     * garbage. Taking the <i>first</i> number instead — as this did — is the version that reads
     * "4 of 4 rules hold: 1.0" as 0.4, runs the loop to {@code maxIterations} on a note that was
     * already perfect, and reports no error at all.
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
     *
     * <p>Only an <b>empty</b> input falls back to the canned blanket. A single item is honoured as
     * a single item: this used to treat "one chunk" as "nothing to fan out over" and quietly
     * substitute five unrelated things, so typing one item on stage produced five answers about
     * food nobody had mentioned — which reads as the demo ignoring you, not as a fallback.
     */
    public static List<String> items(String input) {
        String text = input == null ? "" : input;
        if (text.isBlank()) {
            return BLANKET;
        }
        // Semicolons and newlines win over commas when both are present, because an item can
        // itself contain a comma ("a bar of dark chocolate, most of it"). Splitting on every
        // separator at once fans the mapper out over shrapnel — with no error at all, just a
        // list of answers to half-items.
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
