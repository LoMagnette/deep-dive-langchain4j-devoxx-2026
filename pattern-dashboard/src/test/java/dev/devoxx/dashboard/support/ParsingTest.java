package dev.devoxx.dashboard.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void categorySurvivesAChattyRouter() {
        assertEquals("training", Parsing.category("training"));
        assertEquals("training", Parsing.category("This is best categorised as: **training**."));
        assertEquals("everyday", Parsing.category("Everyday"));
        // The conclusion comes last, so the last label mentioned wins.
        assertEquals("emergency", Parsing.category("Not a training question — this is an emergency."));
        // An unrecognised answer must still pick a destination rather than routing nowhere, and
        // it has to fall towards the vet: that is the mistake you can live with.
        assertEquals("emergency", Parsing.category("no idea"));
        assertEquals("emergency", Parsing.category(null));
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
}
