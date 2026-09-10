package dev.devoxx.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Smoke test over the whole catalog. Runs every registered pattern against the deterministic
 * {@link MockChatModel} and fails on any error event or empty result — the check that turns
 * "the demo broke on stage" into "the build went red".
 */
class PatternCatalogTest {

    /** Runs one pattern on a fresh mock model and returns (result, events). */
    private static Run run(PatternCatalog.PatternDef def) {
        List<RunEvent> events = new ArrayList<>();
        var listener = new StreamingListener(events::add, new AtomicLong());
        String result = def.run(new MockChatModel(), def.defaultInput(), listener);
        return new Run(result, events);
    }

    private record Run(String result, List<RunEvent> events) {
        List<String> errors() {
            return events.stream().filter(e -> "agent-error".equals(e.type()))
                    .map(RunEvent::message).toList();
        }

        List<String> invoked() {
            return events.stream().filter(e -> "agent-before".equals(e.type()))
                    .map(RunEvent::agent).toList();
        }
    }

    @Test
    void everyPatternCompletesUnderTheMockModel() {
        var catalog = new PatternCatalog();
        var failures = new ArrayList<String>();

        for (var info : catalog.infos()) {
            var def = catalog.byId(info.id()).orElseThrow();
            Run r = run(def);
            r.errors().forEach(e -> failures.add(info.id() + " -> " + e));
            if (r.result() == null || r.result().isBlank() || "null".equals(r.result())) {
                // The old conditional-routing bug produced exactly this: no error, no answer.
                failures.add(info.id() + " -> produced no result (" + r.result() + ")");
            }
        }

        assertEquals(13, catalog.infos().size(), "expected all 13 patterns registered");
        assertTrue(failures.isEmpty(), () -> "patterns failed:\n" + String.join("\n", failures));
    }

    @Test
    void loopIteratesThenExitsOnTheScoreBar() {
        var def = new PatternCatalog().byId("loop").orElseThrow();
        Run r = run(def);
        // The mock alternates 0.60 then 0.95, so the scorer must run twice: once below the
        // 0.8 bar, once above it. One invocation would mean the exit condition never gated.
        long scorings = r.invoked().stream().filter("StoryScorer"::equals).count();
        assertEquals(2, scorings, "loop should refine once, then exit: " + r.invoked());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);
    }

    @Test
    void supervisorDelegatesToBothSpecialists() {
        var def = new PatternCatalog().byId("supervisor").orElseThrow();
        Run r = run(def);
        assertTrue(r.invoked().contains("ActivityPlanner"), "no activity planning: " + r.invoked());
        assertTrue(r.invoked().contains("MealPlanner"), "no meal planning: " + r.invoked());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);
    }

    @Test
    void scoreSurvivesAChattyModel() {
        assertEquals(0.85, PatternCatalog.score(scope("score", "0.85")), 1e-9);
        assertEquals(0.85, PatternCatalog.score(scope("score", "I'd rate this **0.85** out of 1.0")), 1e-9);
        assertEquals(0.85, PatternCatalog.score(scope("score", "8.5 out of 10")), 1e-9);
        assertEquals(0.9, PatternCatalog.score(scope("score", "A solid 9/10.")), 1e-9);
        // No number at all must score 0 (keep iterating), never crash.
        assertEquals(0.0, PatternCatalog.score(scope("score", "pretty good, honestly")), 1e-9);
        assertEquals(0.0, PatternCatalog.score(scope("nothing", "")), 1e-9);
    }

    @Test
    void categorySurvivesAChattyRouter() {
        assertEquals("medical", PatternCatalog.category(scope("category", "medical")));
        assertEquals("medical", PatternCatalog.category(
                scope("category", "This request is best categorised as: **medical**.")));
        assertEquals("legal", PatternCatalog.category(scope("category", "Legal")));
        // The conclusion comes last, so the last label mentioned wins.
        assertEquals("medical", PatternCatalog.category(
                scope("category", "Not a legal question — this is medical.")));
        // Unknown answers must still pick a branch rather than silently routing nowhere.
        assertEquals("technical", PatternCatalog.category(scope("category", "no idea")));
    }

    @Test
    void mapperItemsComeFromTheTypedInput() {
        assertEquals(List.of("a", "b", "c"), PatternCatalog.items("a, b, c"));
        assertEquals(List.of("a", "b"), PatternCatalog.items("a;\nb"));
        // A single chunk has nothing to fan out over, so fall back to the canned topics.
        assertEquals(3, PatternCatalog.items("one thing only").size());
    }

    @Test
    void errorsExposeTheRootCauseNotJustTheWrapper() {
        var boom = new RuntimeException("Failed to invoke agent method: write(String)",
                new IllegalStateException("outer", new java.net.ConnectException()));
        String explained = Errors.explain(boom);
        assertTrue(explained.contains("ConnectException"), explained);
        assertTrue(explained.contains("base-url"), "should hint at the fix: " + explained);
    }

    /** A one-key AgenticScope for exercising the parsing helpers. */
    private static AgenticScope scope(String key, String value) {
        var s = dev.langchain4j.agentic.scope.DefaultAgenticScope.ephemeralAgenticScope();
        s.writeState(key, value);
        return s;
    }
}
