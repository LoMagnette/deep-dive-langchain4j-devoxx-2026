package dev.devoxx.dashboard.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import dev.langchain4j.agentic.scope.AgenticScope;
import org.junit.jupiter.api.Test;

/**
 * Every case here is something a real model actually returned. {@link Parsing} exists because
 * each of them broke a pattern in a way that produced no error at all — a score as prose, a
 * category wrapped in a sentence, a list that was not one.
 */
class ParsingTest {

    @Test
    void scoreSurvivesAChattyModel() {
        assertEquals(0.85, Parsing.score(scope("score", "0.85")), 1e-9);
        assertEquals(0.85, Parsing.score(scope("score", "I'd rate this **0.85** out of 1.0")), 1e-9);
        assertEquals(0.85, Parsing.score(scope("score", "8.5 out of 10")), 1e-9);
        assertEquals(0.9, Parsing.score(scope("score", "A solid 9/10.")), 1e-9);
        // No number at all must score 0 (keep iterating), never crash.
        assertEquals(0.0, Parsing.score(scope("score", "pretty good, honestly")), 1e-9);
        assertEquals(0.0, Parsing.score(scope("nothing", "")), 1e-9);
    }

    @Test
    void categorySurvivesAChattyRouter() {
        assertEquals("training", Parsing.category(scope("category", "training")));
        assertEquals("training", Parsing.category(
                scope("category", "This is best categorised as: **training**.")));
        assertEquals("everyday", Parsing.category(scope("category", "Everyday")));
        // The conclusion comes last, so the last label mentioned wins.
        assertEquals("emergency", Parsing.category(
                scope("category", "Not a training question — this is an emergency.")));
        // An unrecognised answer must still pick a destination rather than routing nowhere, and
        // it has to fall towards the vet: that is the mistake you can live with.
        assertEquals("emergency", Parsing.category(scope("category", "no idea")));
    }

    @Test
    void mapperItemsComeFromTheTypedInput() {
        assertEquals(List.of("a", "b", "c"), Parsing.items("a, b, c"));
        assertEquals(List.of("a", "b"), Parsing.items("a;\nb"));
        // Semicolons beat commas when both are present, or an item that contains a comma is
        // fanned out as two half-items with no error at all.
        assertEquals(List.of("a bar of dark chocolate, most of it", "a slice of cheddar"),
                Parsing.items("a bar of dark chocolate, most of it; a slice of cheddar"));
        // A single chunk has nothing to fan out over, so fall back to the canned blanket.
        assertEquals(5, Parsing.items("one thing only").size());
    }

    /** A one-key AgenticScope for exercising the parsing helpers. */
    private static AgenticScope scope(String key, String value) {
        var s = dev.langchain4j.agentic.scope.DefaultAgenticScope.ephemeralAgenticScope();
        s.writeState(key, value);
        return s;
    }
}
