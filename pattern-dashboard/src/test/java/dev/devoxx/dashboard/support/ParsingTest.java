package dev.devoxx.dashboard.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Every case here is something a real model actually returned. {@link Parsing} exists because
 * each of them broke a pattern in a way that produced no error at all — a score as prose, a
 * category wrapped in a sentence, a list that was not one.
 *
 * <p>These take plain strings, not an {@code AgenticScope}: reading the scope is LangChain4j API
 * and stays visible in the demo wiring, so what is left to test here is only the parsing.
 */
class ParsingTest {

    @Test
    void scoreSurvivesAChattyModel() {
        assertEquals(0.85, Parsing.score("0.85"), 1e-9);
        assertEquals(0.85, Parsing.score("I'd rate this **0.85** out of 1.0"), 1e-9);
        assertEquals(0.85, Parsing.score("8.5 out of 10"), 1e-9);
        assertEquals(0.9, Parsing.score("A solid 9/10."), 1e-9);
        // No number at all must score 0 (keep iterating), never crash.
        assertEquals(0.0, Parsing.score("pretty good, honestly"), 1e-9);
        assertEquals(0.0, Parsing.score(""), 1e-9);
        // An absent key reads as null through the scope, and must not blow up a loop's predicate.
        assertEquals(0.0, Parsing.score(null), 1e-9);
    }

    /**
     * The critic is asked for "the fraction of rules that hold", over four named rules — so it
     * answers in fractions, and the numerator on its own is not the score.
     *
     * <p>The last case is the one that matters and the one that was wrong: reading the FIRST
     * number turns a note that passes all four rules into 0.4, the exit condition never fires,
     * and the loop runs to {@code maxIterations} on a note that was already finished. No error,
     * no failing test — just a demo that looks like a critic nobody can satisfy.
     */
    @Test
    void scoreReadsTheFractionTheCriticWasAskedFor() {
        assertEquals(0.75, Parsing.score("3/4"), 1e-9);
        assertEquals(0.75, Parsing.score("3 of 4"), 1e-9);
        assertEquals(0.75, Parsing.score("3 of 4 rules hold."), 1e-9);
        assertEquals(0.5, Parsing.score("2 of 4 → 0.5"), 1e-9);
        assertEquals(1.0, Parsing.score("4 of 4 rules hold: 1.0"), 1e-9);
    }

    /**
     * A leading number that is not the score. Models state the conclusion last, so the last
     * number already in range wins over a rule index mentioned on the way there.
     */
    @Test
    void scoreIgnoresARuleNumberOnTheWayToTheAnswer() {
        assertEquals(0.4, Parsing.score("Rule 3 fails, so 0.4"), 1e-9);
        assertEquals(0.6, Parsing.score("Score: 0.60"), 1e-9);
        // Nothing in range at all: rescale the last number, which is what turns 85% into 0.85.
        assertEquals(0.85, Parsing.score("85%"), 1e-9);
    }

    @Test
    void categorySurvivesAChattyClassifier() {
        assertEquals("lost", Parsing.category("lost"));
        assertEquals("underground", Parsing.category("This is best categorised as: **underground**."));
        assertEquals("urgent", Parsing.category("Urgent"));
        // The conclusion comes last, so the last label mentioned wins.
        assertEquals("hurt", Parsing.category("Not lost — someone is hurt."));
        // An unrecognised answer must still pick a destination rather than routing nowhere, and
        // it has to fall towards Doc: sending the medic is the mistake you can live with.
        assertEquals("hurt", Parsing.category("no idea"));
        assertEquals("hurt", Parsing.category(null));
    }

    @Test
    void fifisScoreIsTheNumberAfterTheLabelNotOneInHerFeedback() {
        assertEquals(0.5, Parsing.reviewScore("SCORE: 2/4\nFEEDBACK: fix 1 thing and the 2 o'clock"), 1e-9);
        assertEquals(1.0, Parsing.reviewScore("**SCORE:** 4/4 FEEDBACK: fine"), 1e-9);
        assertEquals(0.75, Parsing.reviewScore("Score = 0.75, because 1 rule fails"), 1e-9);
        // No label at all: fall back to reading the review like any other score.
        assertEquals(0.8, Parsing.reviewScore("I would give it 0.8"), 1e-9);
        assertEquals(0.0, Parsing.reviewScore(null), 1e-9);
    }

    @Test
    void foundReadsTheMarkerAndNotItsNegation() {
        assertTrue(Parsing.found("FOUND: in the middle, eating the scarecrow's hat"));
        assertTrue(Parsing.found("Found: the goat"));
        assertFalse(Parsing.found("Not found: nothing in the east loops"));
        assertFalse(Parsing.found("still looking"));
        assertFalse(Parsing.found(null));
    }

    @Test
    void firstNumberReadsMeasurementsOutOfSentences() {
        assertEquals(6.0, Parsing.firstNumber("the branch is 6 metres up", 0), 1e-9);
        assertEquals(12.5, Parsing.firstNumber("ice 12,5 cm thick", 0), 1e-9);
        assertEquals(9.0, Parsing.firstNumber("no numbers here", 9), 1e-9);
    }

    @Test
    void mapperItemsComeFromTheTypedInput() {
        assertEquals(List.of("a", "b", "c"), Parsing.items("a, b, c"));
        assertEquals(List.of("a", "b"), Parsing.items("a;\nb"));
        // Semicolons beat commas when both are present, or an item that contains a comma is
        // fanned out as two half-items with no error at all.
        assertEquals(List.of("Puddle, the small one", "Pickle"),
                Parsing.items("Puddle, the small one; Pickle"));
        // A single item is honoured as a single item: substituting eight canned ducklings for the
        // one the speaker typed reads as the demo ignoring them.
        assertEquals(List.of("one duckling only"), Parsing.items("one duckling only"));
        // Only a genuinely empty input falls back to the eight ducklings — the mapper must always
        // have something to fan out over.
        assertEquals(8, Parsing.items("").size());
        assertEquals(8, Parsing.items("   ").size());
        assertEquals(8, Parsing.items(null).size());
        assertEquals(8, Parsing.items(" ; , ").size());
    }
}
