package dev.devoxx.dashboard.catalog;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.devoxx.dashboard.model.MockChatModel;
import dev.devoxx.dashboard.run.RunEvent;
import dev.devoxx.dashboard.run.StreamingListener;
import org.junit.jupiter.api.Test;

/**
 * Smoke test over the whole catalog. Runs every registered pattern against the deterministic
 * {@link dev.devoxx.dashboard.model.MockChatModel} and fails on any error event or empty result — the check that turns
 * "the demo broke on stage" into "the build went red".
 */
class PatternCatalogTest {

    /**
     * Runs one pattern on a fresh mock model and returns (result, events).
     *
     * <p>The list must be synchronized: parallel and mapper patterns invoke their agents on
     * several threads, so the listener fires concurrently. A plain ArrayList silently drops
     * events here, which shows up as a flaky "that agent never ran" failure.
     */
    private static Run run(PatternDef def) {
        return run(def, def.defaultInput());
    }

    /** Same, with a typed-in input — for patterns whose behaviour depends on what is asked. */
    private static Run run(PatternDef def, String input) {
        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        var listener = new StreamingListener(events::add, new AtomicLong());
        String result = def.run(new MockChatModel(), input, listener);
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

        assertEquals(14, catalog.infos().stream()
                        .filter(i -> !i.category().equals("composite")).count(),
                "expected all 14 patterns registered");
        assertTrue(failures.isEmpty(), () -> "patterns failed:\n" + String.join("\n", failures));
    }

    /**
     * The capstone is the one entry that is a system rather than a pattern, so what matters is
     * that the composition actually holds together: every stage runs, and each one is reached
     * through the key the previous stage wrote.
     */
    @Test
    void theCompositeRunsEveryStageItAdvertises() {
        var def = new PatternCatalog().byId("sitterNote").orElseThrow();
        Run r = run(def);
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        var invoked = r.invoked();
        assertTrue(invoked.contains("WorryRouter"), "no triage: " + invoked);
        assertTrue(invoked.stream().anyMatch(a -> a.equals("EmergencyVet")
                        || a.equals("DogTrainer") || a.equals("EverydayCare")),
                "routing reached nobody: " + invoked);
        assertTrue(invoked.contains("MealPlanner") && invoked.contains("WalkPlanner"),
                "the parallel step did not fan out: " + invoked);
        assertTrue(invoked.contains("SitterNoteMerger"), "nothing merged the parts: " + invoked);
        // The mock alternates 0.60 then 0.95, so a working exit condition scores exactly twice.
        assertEquals(2, invoked.stream().filter("FridgeRuleCheck"::equals).count(),
                "refinement loop should iterate once then exit: " + invoked);

        assertTrue(r.result() != null && !r.result().isBlank(), "no sitter note produced");
    }

    @Test
    void loopIteratesThenExitsOnTheScoreBar() {
        var def = new PatternCatalog().byId("loop").orElseThrow();
        Run r = run(def);
        // The mock alternates 0.60 then 0.95, so the scorer must run twice: once below the
        // 0.8 bar, once above it. One invocation would mean the exit condition never gated.
        long scorings = r.invoked().stream().filter("FridgeRuleCheck"::equals).count();
        assertEquals(2, scorings, "loop should refine once, then exit: " + r.invoked());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);
    }

    @Test
    void supervisorDelegatesToBothSpecialists() {
        var def = new PatternCatalog().byId("supervisor").orElseThrow();
        Run r = run(def);
        assertTrue(r.invoked().contains("RoutinePlanner"),
                "no routine planning: " + r.invoked());
        assertTrue(r.invoked().contains("TrainingPlanner"),
                "no training planning: " + r.invoked());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);
    }

    @Test
    void theCouncilCarriesTwoZooPatternsEndToEnd() {
        var def = new PatternCatalog().byId("secondDogCouncil").orElseThrow();
        Run r = run(def);
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        var invoked = r.invoked();
        // The mapper fans one agent over three angles, so the scout is invoked more than once.
        assertTrue(invoked.stream().filter(a -> a.startsWith("AngleScout")).count() >= 3,
                "the mapper did not scatter: " + invoked);
        assertTrue(invoked.contains("CouncilBriefer"), "findings were never turned into a motion");
        assertTrue(invoked.contains("SecondDogFor") && invoked.contains("SecondDogAgainst"),
                "the debate did not happen: " + invoked);
        assertTrue(invoked.contains("HouseholdVerdict"), "nobody ruled: " + invoked);
        assertTrue(invoked.contains("SpaceAndTime") && invoked.contains("MoneyAndVet")
                        && invoked.contains("AskZaoHimself"),
                "the vote did not reach all three assessors: " + invoked);
        assertTrue(r.result() != null && !r.result().isBlank(), "no ruling produced");
    }

    /**
     * The demos have to demonstrate something, and the audience has to be able to tell. Every
     * assertion here is a claim the speaker makes out loud, and each one used to be false of
     * this dashboard: a vote whose voters could not disagree, a scatter/gather whose items came
     * back identical, a "planner" with only one possible order to find. If one of these goes
     * red, a prompt change has quietly turned a pattern back into decoration — which no "it ran
     * without erroring" test would notice.
     */
    @Test
    void theDemoProblemsActuallyDemonstrateTheirPattern() {
        var catalog = new PatternCatalog();

        // Walk him now? Both checks run, and it is the JOIN that vetoes — a fan-out demo whose
        // combiner only concatenates is missing half the pattern.
        Run walk = run(catalog.byId("parallel").orElseThrow());
        assertTrue(walk.invoked().containsAll(List.of("WeatherCheck", "DogCheck")),
                "both checks must run: " + walk.invoked());
        assertTrue(walk.result().startsWith("Not now"),
                "hot pavement must veto the walk: " + walk.result());

        // A dog that has eaten chocolate must reach the vet. Routing that to the trainer is
        // precisely the mistake conditional routing is here to prevent, and the room knows it.
        Run worry = run(catalog.byId("conditional").orElseThrow());
        assertTrue(worry.invoked().contains("EmergencyVet"),
                "a poisoning must reach the vet: " + worry.invoked());

        // GOAP's agents are registered backwards on purpose, so the only way to get this order
        // is for the planner to have derived it from the declared I/O keys.
        List<String> recall = run(catalog.byId("goap").orElseThrow()).invoked();
        assertTrue(recall.indexOf("IndoorRecall") < recall.indexOf("GardenRecall"),
                "the garden step cannot come before the indoor step: " + recall);
        assertTrue(recall.indexOf("GardenRecall") < recall.indexOf("ParkRecall"),
                "the park step comes last: " + recall);

        // Five things off the blanket, five verdicts, and they must NOT all be the same — the
        // room knows the cheddar is fine and the grapes are not.
        List<String> verdicts = run(catalog.byId("parallelMapper").orElseThrow())
                .result().lines().toList();
        assertEquals(5, verdicts.size(), "one verdict per item: " + verdicts);
        assertTrue(verdicts.stream().anyMatch(v -> v.contains("Dangerous")),
                "the grapes must be flagged: " + verdicts);
        assertTrue(verdicts.stream().anyMatch(v -> v.contains("Fine")),
                "the cheddar must be cleared: " + verdicts);

        // BDI orders by priority and precondition, not by declaration order. Nobody needs to be
        // told a puppy goes out before he is fed and long before he is taught anything.
        List<String> hour = run(catalog.byId("bdi").orElseThrow()).invoked();
        assertTrue(hour.indexOf("ToiletTrip") < hour.indexOf("FirstMeal"),
                "out before food: " + hour);
        assertTrue(hour.indexOf("FirstMeal") < hour.indexOf("FirstTraining"),
                "fed before taught: " + hour);

        // The refinement loop has to actually fix the note it was given: rule 3 is the vet's
        // number, and the input deliberately does not have one.
        String note = run(catalog.byId("loop").orElseThrow()).result();
        assertTrue(note.contains("061 22 33 44"),
                "the loop did not bring the note up to the rules: " + note);

        // Every assessor votes, the result shows each vote separately, and they SPLIT. Three
        // agents that always agree make the tally decoration.
        Run ballot = run(catalog.byId("voting").orElseThrow());
        assertTrue(ballot.invoked().containsAll(List.of("SpaceAndTime", "MoneyAndVet",
                "AskZaoHimself")), "the vote did not reach all three criteria: "
                + ballot.invoked());
        List<String> votes = ballot.result().lines().filter(l -> l.startsWith("- ")).toList();
        assertEquals(3, votes.size(), "each criterion's own vote must be visible: " + votes);
        assertTrue(votes.stream().anyMatch(v -> v.contains("YES"))
                        && votes.stream().anyMatch(v -> v.contains("LATER")),
                "the three criteria must be able to disagree: " + votes);
        assertTrue(ballot.result().startsWith("**Majority: LATER"),
                "two of three said later, so that is the majority: " + ballot.result());
    }

    /**
     * The hand-written planner is the one pattern whose whole point is a decision made in Java
     * rather than by a builder or a model, so its policy is asserted directly: the ladder must
     * stop at the first rung that can answer. A planner that always walks every tier is a
     * sequence with extra ceremony, and it would pass every other test in this file.
     */
    @Test
    void theCustomPlannerStopsAtTheFirstRungThatCanAnswer() {
        var def = new PatternCatalog().byId("customPlanner").orElseThrow();

        // A limp is past the book and past the trainer, so the ladder runs to the top.
        Run medical = run(def);
        assertTrue(medical.errors().isEmpty(), medical.errors()::toString);
        assertEquals(List.of("PuppyBook", "TrainerOnCall", "VetOnCall"),
                medical.invoked().stream().filter(a -> !a.equals("invoke")).toList(),
                "a limp should escalate all the way, in cost order");

        // Ordinary kibble question: the cheapest rung answers it and nothing else is called.
        // This is the assertion that distinguishes the planner from a sequence.
        Run basics = run(def, "which food should I buy for a four-year-old shepherd?");
        assertEquals(List.of("PuppyBook"), basics.invoked().stream()
                        .filter(a -> !a.equals("invoke")).toList(),
                "the book answered, so nobody should have rung the trainer or the vet");
        assertTrue(basics.result() != null && !basics.result().isBlank(), "no answer returned");

        // A behaviour question stops one rung further up — never reaching the vet.
        Run behaviour = run(def, "he pulls like a train on the lead");
        assertEquals(List.of("PuppyBook", "TrainerOnCall"), behaviour.invoked().stream()
                        .filter(a -> !a.equals("invoke")).toList(),
                "the trainer answered, so the vet should not have been rung");
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

        // The escalation ladder must show BOTH ways out of every rung — on up, and out to the
        // answer. Drawn as a plain chain it would read as a pipeline that always runs all three,
        // which is the exact misreading the pattern exists to correct.
        String exit = role(catalog, "customPlanner", "join");
        assertEquals(3, inDegree(catalog, "customPlanner", exit),
                "every rung needs its own way out, or the picture says only the vet can answer");
        assertEquals(2, edges(catalog, "customPlanner").stream()
                        .filter(e -> "ESCALATE".equals(e.label())).count(),
                "the two lower rungs escalate; the top one has nowhere to escalate to");

        // Supervisor and blackboard are loops, not one-way arrows.
        assertTrue(mutual(catalog, "supervisor", "supervisor", "routine"),
                "supervisor invokes the planner and reads its result back");
        assertTrue(mutual(catalog, "blackboard", "walks", "board"),
                "blackboard contributors read as well as write");
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

}
