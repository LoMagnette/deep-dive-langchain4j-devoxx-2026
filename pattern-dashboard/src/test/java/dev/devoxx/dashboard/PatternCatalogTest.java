package dev.devoxx.dashboard;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        assertEquals(13, catalog.infos().stream()
                        .filter(i -> !i.category().equals("composite")).count(),
                "expected all 13 patterns registered");
        assertTrue(failures.isEmpty(), () -> "patterns failed:\n" + String.join("\n", failures));
    }

    /**
     * The capstone is the one entry that is a system rather than a pattern, so what matters is
     * that the composition actually holds together: every stage runs, and each one is reached
     * through the key the previous stage wrote.
     */
    @Test
    void theCompositeRunsEveryStageItAdvertises() {
        var def = new PatternCatalog().byId("kennelDesk").orElseThrow();
        Run r = run(def);
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        var invoked = r.invoked();
        assertTrue(invoked.contains("KennelRouter"), "no triage: " + invoked);
        assertTrue(invoked.stream().anyMatch(a -> a.endsWith("Expert")),
                "routing reached no specialist: " + invoked);
        assertTrue(invoked.contains("ActivityPlanner") && invoked.contains("MealPlanner"),
                "the parallel step did not fan out: " + invoked);
        assertTrue(invoked.contains("CarePlanWriter"), "nothing merged the findings: " + invoked);
        // The mock alternates 0.60 then 0.95, so a working exit condition scores exactly twice.
        assertEquals(2, invoked.stream().filter("PlanCritic"::equals).count(),
                "refinement loop should iterate once then exit: " + invoked);

        assertTrue(r.result() != null && !r.result().isBlank(), "no plan produced");
    }

    @Test
    void loopIteratesThenExitsOnTheScoreBar() {
        var def = new PatternCatalog().byId("loop").orElseThrow();
        Run r = run(def);
        // The mock alternates 0.60 then 0.95, so the scorer must run twice: once below the
        // 0.8 bar, once above it. One invocation would mean the exit condition never gated.
        long scorings = r.invoked().stream().filter("PackCritic"::equals).count();
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
        assertEquals("veterinary", PatternCatalog.category(scope("category", "veterinary")));
        assertEquals("veterinary", PatternCatalog.category(
                scope("category", "This request is best categorised as: **veterinary**.")));
        assertEquals("nutrition", PatternCatalog.category(scope("category", "Nutrition")));
        // The conclusion comes last, so the last label mentioned wins.
        assertEquals("veterinary", PatternCatalog.category(
                scope("category", "Not a behaviour question — this is veterinary.")));
        // Unknown answers must still pick a branch rather than silently routing nowhere.
        assertEquals("behaviour", PatternCatalog.category(scope("category", "no idea")));
    }

    @Test
    void mapperItemsComeFromTheTypedInput() {
        assertEquals(List.of("a", "b", "c"), PatternCatalog.items("a, b, c"));
        assertEquals(List.of("a", "b"), PatternCatalog.items("a;\nb"));
        // A single chunk has nothing to fan out over, so fall back to the canned topics.
        assertEquals(3, PatternCatalog.items("one thing only").size());
    }

    /**
     * A topology has to show the mechanism, not just the cast. These are the structural claims
     * each diagram makes; the geometry that renders them lives in the frontend.
     */
    @Test
    void everyTopologyShowsWhatItsPatternActuallyDoes() {
        var catalog = new PatternCatalog();
        var problems = new ArrayList<String>();

        for (var info : catalog.infos()) {
            var ids = info.topology().nodes().stream().map(Topology.Node::id).collect(toSet());
            var touched = new java.util.HashSet<String>();
            for (var e : info.topology().edges()) {
                if (!ids.contains(e.from()) || !ids.contains(e.to())) {
                    problems.add(info.id() + ": edge " + e.from() + "->" + e.to() + " goes nowhere");
                }
                touched.add(e.from());
                touched.add(e.to());
            }
            ids.stream().filter(id -> !touched.contains(id))
                    .forEach(id -> problems.add(info.id() + ": '" + id + "' is drawn unconnected"));
        }
        assertTrue(problems.isEmpty(), () -> String.join("\n", problems));

        // Fan-out without a join draws work being split and never brought back together.
        assertEquals(2, inDegree(catalog, "parallel", role(catalog, "parallel", "join")),
                "both branches must feed the parallel combiner");
        assertEquals(3, inDegree(catalog, "voting", role(catalog, "voting", "join")),
                "every voter must feed the tally");
        assertEquals(1, inDegree(catalog, "parallelMapper",
                role(catalog, "parallelMapper", "join")), "mapped work must be gathered");

        // Routing: one router, one edge per labelled alternative, and no join — only one runs.
        String router = role(catalog, "conditional", "router");
        var routed = edges(catalog, "conditional").stream()
                .filter(e -> e.from().equals(router)).toList();
        assertEquals(3, routed.size(), "router should offer three alternatives");
        assertTrue(routed.stream().allMatch(e -> e.label() != null && !e.label().isBlank()),
                "each branch must say which category picks it");
        assertNull(role(catalog, "conditional", "join"),
                "only one branch runs, so a join would misrepresent it");

        // Supervisor and blackboard are loops, not one-way arrows.
        assertTrue(mutual(catalog, "supervisor", "supervisor", "activity"),
                "supervisor invokes the planner and reads its result back");
        assertTrue(mutual(catalog, "blackboard", "tracker", "scope"),
                "blackboard experts read as well as write");
    }

    private static List<Topology.Edge> edges(PatternCatalog c, String id) {
        return c.byId(id).orElseThrow().topology().edges();
    }

    /** Id of the single node with this role, or null when the pattern has none. */
    private static String role(PatternCatalog c, String id, String role) {
        return c.byId(id).orElseThrow().topology().nodes().stream()
                .filter(n -> n.role().equals(role)).map(Topology.Node::id).findFirst().orElse(null);
    }

    private static long inDegree(PatternCatalog c, String id, String node) {
        return edges(c, id).stream().filter(e -> e.to().equals(node)).count();
    }

    private static boolean mutual(PatternCatalog c, String id, String a, String b) {
        var es = edges(c, id);
        return es.stream().anyMatch(e -> e.from().equals(a) && e.to().equals(b))
                && es.stream().anyMatch(e -> e.from().equals(b) && e.to().equals(a));
    }

    @Test
    void scopeEntriesCarryAReadableTypeAndSize() {
        var text = StreamingListener.describe("hello");
        assertEquals("String", text.type());
        assertEquals(5, (int) text.size());

        // List.of(...) is really an ImmutableCollections$ListN; the variables table must not
        // leak that at the audience.
        var list = StreamingListener.describe(List.of("a", "b", "c"));
        assertEquals("List", list.type());
        assertEquals(3, (int) list.size());

        // Sizeless scalars simply have no size, rather than a misleading 0.
        var number = StreamingListener.describe(42);
        assertEquals("Integer", number.type());
        assertNull(number.size());

        assertEquals("null", StreamingListener.describe(null).type());
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
