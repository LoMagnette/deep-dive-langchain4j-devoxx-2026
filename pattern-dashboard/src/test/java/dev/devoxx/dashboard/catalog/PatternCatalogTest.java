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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.devoxx.dashboard.run.AskHuman;
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
        return run(def, input, AskHuman.NOBODY);
    }

    /**
     * Same, with a stand-in for the person. This is the only way to test a pattern that stops and
     * waits for a human: swap the human for a lambda. It is also why {@link AskHuman} is an
     * interface rather than a method on the web layer.
     */
    private static Run run(PatternDef def, String input, AskHuman human) {
        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        var listener = new StreamingListener(events::add, new AtomicLong(), human);
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

        assertEquals(15, catalog.infos().stream()
                        .filter(i -> !i.category().equals("composite")).count(),
                "expected all 15 patterns registered");
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

    /**
     * The claim is not "it calls more than one agent" — a fan-out does that. It is that the
     * <b>second call exists because of what the first one said</b>, which neither a router nor a
     * fan-out can produce.
     */
    @Test
    void theSupervisorCallsASecondAgentBecauseOfWhatTheFirstSaid() {
        var def = new PatternCatalog().byId("supervisor").orElseThrow();
        Run r = run(def);
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        var called = r.invoked().stream()
                .filter(a -> List.of("TriageNurse", "EverydayCare", "DogTrainer", "EmergencyVet")
                        .contains(a))
                .toList();
        assertEquals(List.of("TriageNurse", "EmergencyVet"), called,
                "the nurse takes the call, and who she names is called next: " + r.invoked());

        // One answer with a route, not a set of opinions — printing every call as a peer block
        // is what a parallel workflow produces, and it made this demo read as one.
        assertTrue(r.result().startsWith("**TriageNurse → EmergencyVet**"),
                "the route has to lead, as a chain: " + r.result());
        assertTrue(r.result().contains("named EmergencyVet, so that is who the supervisor called"),
                "the result must say why the second call happened: " + r.result());
        // The protocol words the planner acts on must never reach the reader.
        assertTrue(!r.result().contains("NEEDS:") && !r.result().contains("ESCALATE"),
                "protocol markers leaked into the answer: " + r.result());
        int answerAt = r.result().indexOf("The nurse is right to send him");
        int reasonAt = r.result().indexOf("did not answer it");
        assertTrue(answerAt > 0 && reasonAt > answerAt,
                "the final answer must come first and the route beneath it: " + r.result());

        // Who she names decides who is called — not a script. A behaviour problem goes to the
        // trainer instead, on the same wiring.
        var behaviour = run(def, "he pulls like a train on the lead and barks at the postman")
                .invoked().stream()
                .filter(a -> List.of("TriageNurse", "EverydayCare", "DogTrainer", "EmergencyVet")
                        .contains(a))
                .toList();
        assertEquals(List.of("TriageNurse", "DogTrainer"), behaviour,
                "the same run should reach a different specialist: " + behaviour);

        // And when nobody else is needed it stops, or "it called two" is just a longer script.
        var settled = run(def, "he ate a bit of grass and was sick once, then asked for his tea")
                .invoked().stream()
                .filter(a -> List.of("TriageNurse", "EverydayCare", "DogTrainer", "EmergencyVet")
                        .contains(a))
                .toList();
        assertEquals(List.of("TriageNurse"), settled,
                "the nurse settled this one, so nobody else should have been called: " + settled);
    }

    /**
     * Every assertion here is a claim the speaker makes out loud. If one goes red, a prompt
     * change has turned a pattern back into decoration — which no "it ran without erroring"
     * test would notice.
     */
    @Test
    void theDemoProblemsActuallyDemonstrateTheirPattern() {
        var catalog = new PatternCatalog();

        // Both halves of the note get planned at once and the join brings them back — a fan-out
        // that never rejoins is only half the pattern.
        Run halves = run(catalog.byId("parallel").orElseThrow());
        assertTrue(halves.invoked().containsAll(List.of("MealPlanner", "WalkPlanner")),
                "both halves must be planned: " + halves.invoked());
        assertTrue(halves.result().contains("Meals") && halves.result().contains("Walks"),
                "the join must bring both halves back together: " + halves.result());

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
        assertEquals(List.of("EverydayCare", "DogTrainer", "EmergencyVet"),
                medical.invoked().stream().filter(a -> !a.equals("invoke")).toList(),
                "a limp should escalate all the way, in cost order");

        // Ordinary kibble question: the cheapest rung answers it and nothing else is called.
        // This is the assertion that distinguishes the planner from a sequence.
        Run basics = run(def, "which food should I buy for a four-year-old shepherd?");
        assertEquals(List.of("EverydayCare"), basics.invoked().stream()
                        .filter(a -> !a.equals("invoke")).toList(),
                "everyday care answered, so nobody should have rung the trainer or the vet");
        assertTrue(basics.result() != null && !basics.result().isBlank(), "no answer returned");

        // A behaviour question stops one rung further up — never reaching the vet.
        Run behaviour = run(def, "he pulls like a train on the lead");
        assertEquals(List.of("EverydayCare", "DogTrainer"), behaviour.invoked().stream()
                        .filter(a -> !a.equals("invoke")).toList(),
                "the trainer answered, so the vet should not have been rung");
    }

    /**
     * The human-in-the-loop demo is the one pattern whose answer is supposed to change because a
     * person said so, and the only way to test that is to stand in for the person. Both paths
     * matter: a gate that cannot refuse is a rubber stamp, and a gate whose refusal is quietly
     * overridden downstream is worse than no gate at all.
     */
    @Test
    void theHumanCanRefuseAndTheRunHonoursIt() {
        var def = new PatternCatalog().byId("humanApproval").orElseThrow();

        // The question has to reach the person with the draft in it — an approval step that asks
        // "is this ok?" without showing what "this" is, is theatre.
        var asked = new ArrayList<String>();
        Run approved = run(def, def.defaultInput(), q -> {
            asked.add(q);
            return "Yes, but also tell her to take a photo of the wrapper first.";
        });
        assertTrue(approved.errors().isEmpty(), approved.errors()::toString);
        assertEquals(1, asked.size(), "the person should be asked exactly once: " + asked);
        assertTrue(asked.get(0).contains("wrapper"),
                "the question must carry the draft being approved: " + asked.get(0));
        assertTrue(approved.invoked().contains("WorryRouter"),
                "the approval demo is the routing demo plus a person: " + approved.invoked());

        // Refusal has to stick. Assert on the INSTRUCTION, not the whole result: the result also
        // echoes what the person said, so a naive contains() passes on their own words and a
        // run that ignored them entirely still looks green.
        Run refused = run(def, def.defaultInput(), q -> "No. Do not ring anyone, wait for me.");
        assertTrue(instruction(refused).contains("Do not act on it"),
                "a refusal must survive to the instruction: " + instruction(refused));
        assertTrue(!instruction(refused).contains("Ring the practice now"),
                "a refusal must not be quietly overridden: " + instruction(refused));

        // And the run has to be legible on the page: a question event, then an answer event.
        List<String> types = refused.events().stream().map(RunEvent::type).toList();
        assertTrue(types.contains("human-ask") && types.contains("human-answer"),
                "the page needs both halves of the exchange: " + types);
    }

    /** Just the final instruction, without the draft and the answer the result also shows. */
    private static String instruction(Run r) {
        String marker = "**So the sitter is told**";
        int at = r.result().indexOf(marker);
        return at < 0 ? r.result() : r.result().substring(at + marker.length());
    }

    /**
     * A parallel step really does overlap: against a model where every call costs 150ms, two
     * branches cost 300ms of agent time inside a run barely longer than one of them. Also pins
     * the keying — durations are per {@code agentId()}, and colliding ids would make the
     * mapper's five items report garbage.
     */
    @Test
    void everyStepIsTimedAndParallelStepsActuallyOverlap() {
        var catalog = new PatternCatalog();
        long delay = 150;

        // Two independent checks; run one after another they would cost ~300ms.
        var parallel = catalog.byId("parallel").orElseThrow();
        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        parallel.run(slowModel(delay), parallel.defaultInput(),
                new StreamingListener(events::add, new AtomicLong()));

        // The Parallel step is itself reported as an agent, so its own duration is the wall clock
        // of the fan-out — a better number to assert on than anything measured out here.
        List<RunEvent> done = events.stream()
                .filter(e -> "agent-after".equals(e.type())).toList();
        assertTrue(done.stream().allMatch(e -> e.millis() != null),
                "every agent-after must carry a duration: " + done.stream()
                        .map(e -> e.agent() + "=" + e.millis()).toList());

        List<RunEvent> branches = done.stream()
                .filter(e -> e.agent().endsWith("Planner")).toList();
        assertEquals(2, branches.size(), "both halves should have been planned: " + done.stream()
                .map(RunEvent::agent).toList());
        assertTrue(branches.stream().allMatch(e -> e.millis() >= delay),
                "a branch cannot finish faster than the model it called: " + branches.stream()
                        .map(RunEvent::millis).toList());

        long sum = branches.stream().mapToLong(RunEvent::millis).sum();
        long step = done.stream().filter(e -> "Parallel".equals(e.agent()))
                .mapToLong(RunEvent::millis).max().orElseThrow();
        assertTrue(step < sum * 0.8,
                "the two branches must overlap: the step took " + step + "ms against " + sum
                        + "ms of agent time");

        // A fan-out over five items must report five distinct durations, not one reused.
        var mapper = catalog.byId("parallelMapper").orElseThrow();
        List<RunEvent> mapped = Collections.synchronizedList(new ArrayList<>());
        mapper.run(new MockChatModel(), mapper.defaultInput(),
                new StreamingListener(mapped::add, new AtomicLong()));
        long timed = mapped.stream().filter(e -> "agent-after".equals(e.type())
                && e.agent().startsWith("FoodSafetyCheck") && e.millis() != null).count();
        assertEquals(5, timed, "each mapped item needs its own timing: " + mapped.stream()
                .filter(e -> "agent-after".equals(e.type())).map(RunEvent::agent).toList());
    }

    /** A model that takes its time, so a duration has something to measure. */
    private static ChatModel slowModel(long millis) {
        return new MockChatModel() {
            @Override
            public ChatResponse chat(ChatRequest request) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.chat(request);
            }
        };
    }

    /**
     * A new demo cannot join the catalogue without a beat. The beats are written to fit the rail
     * order, which is the <b>autonomy dial</b> — if a better story seems to want the patterns
     * reordered, the story is what is wrong.
     */
    @Test
    void everyDemoHasItsBeatInTheNarration() {
        var missing = new ArrayList<String>();
        for (var info : new PatternCatalog().infos()) {
            if (info.story() == null || info.story().isBlank()) {
                missing.add(info.id() + " has no story");
            } else if (info.story().length() > 140) {
                // A beat is a sentence the speaker says out loud, not a paragraph they read.
                missing.add(info.id() + " reads as a paragraph (" + info.story().length() + " chars)");
            }
        }
        assertTrue(missing.isEmpty(), () -> String.join("\n", missing));
    }

    /**
     * The hand-off lives in two files that have to agree and nothing at run time forces them to:
     * the nurse's prompt must name who is needed, and the supervisor's context must say what to
     * do with that. When they disagree a live planner calls one agent and stops, and every other
     * test here still passes — the mock cannot see it.
     */
    @Test
    void theHandOffIsSpelledOutWhereThePlannerAndTheTrainerCanBothSeeIt() {
        var catalog = new PatternCatalog();

        // The supervisor must explain what an escalation IS — a planner cannot act on a marker
        // nobody has defined for it.
        String useful = catalog.byId("supervisor").orElseThrow().useful();
        assertTrue(useful.contains("that answer is what makes"),
                "the supervisor's own description must state the dependency: " + useful);

        // And the trainer has to be told to refuse the case, or it will helpfully answer it and
        // there will be nothing to hand on. The rule is in its @UserMessage.
        String prompt = dev.devoxx.dashboard.demos.conditional.DogTrainer.class
                .getMethods()[0].getAnnotation(dev.langchain4j.service.UserMessage.class)
                .value()[0];
        assertTrue(prompt.contains("ESCALATE"),
                "the trainer must have a way to decline: " + prompt);
        assertTrue(prompt.toLowerCase().contains("until a vet"),
                "the trainer must be told WHEN to decline, or it will just answer: " + prompt);
    }

    /**
     * Scope keys are declared as {@code TypedKey} records, never as string literals at the call
     * site. This reads the demo sources and fails on a relapse, because a stringly-typed key is
     * invisible until it is wrong at run time — this repo lost a run to {@code "note"} against
     * {@code "notes"}, and another to findings declared {@code String} when the scope held a
     * {@code List}. The compiler cannot see either mistake; this can.
     */
    @Test
    void noDemoAddressesTheScopeWithAStringLiteral() throws Exception {
        var demos = java.nio.file.Path.of("src/main/java/dev/devoxx/dashboard/demos");
        var offenders = new ArrayList<String>();
        // outputKey("x"), readState("x"), hasState("x"), itemsProvider("x") — every place the
        // library will take a String and silently accept a typo.
        var stringKey = java.util.regex.Pattern.compile(
                "\\.(outputKey|readState|hasState|itemsProvider)\\(\"");
        try (var paths = java.nio.file.Files.walk(demos)) {
            for (var p : paths.filter(p -> p.toString().endsWith("Pattern.java")).toList()) {
                var src = java.nio.file.Files.readString(p);
                var m = stringKey.matcher(src);
                while (m.find()) {
                    offenders.add(p.getFileName() + " uses " + m.group(1) + "(\"…\")");
                }
            }
        }
        assertTrue(offenders.isEmpty(), () -> "use a TypedKey from Keys instead:\n"
                + String.join("\n", offenders));
    }

    /**
     * The demos are meant to build on each other: by the capstone, nearly every box on the
     * diagram is something the room has already watched run on its own. That claim is made in
     * prose on every page, so it had better be true of the wiring — and it is the first thing a
     * refactor would quietly break.
     */
    @Test
    void theDemosReuseWhatTheEarlierOnesBuilt() {
        var catalog = new PatternCatalog();

        // The three desks are introduced by routing and then reused by four later demos, each
        // putting a different control flow around the same cast. That progression is the spine
        // of the middle of the talk.
        for (String id : List.of("conditional", "humanApproval", "supervisor", "customPlanner",
                "sitterNote")) {
            assertTrue(labels(catalog, id).containsAll(List.of("EverydayCare", "DogTrainer",
                            "EmergencyVet")),
                    id + " should be built from the three desks: " + labels(catalog, id));
        }

        // The sitter-note spine: one agent introduced in demo 2, put in a loop in demo 3, and
        // used a third time by the capstone.
        for (String id : List.of("sequential", "loop", "sitterNote")) {
            assertTrue(labels(catalog, id).contains("FridgeChecklist"),
                    id + " should reuse the checklist agent: " + labels(catalog, id));
        }

        // The capstone's fan-out is demo 4's, unchanged.
        assertTrue(labels(catalog, "sitterNote").containsAll(List.of("MealPlanner", "WalkPlanner")),
                "the capstone should reuse the parallel demo's planners");

        // And the council ratifies with the very assessors that voted two demos earlier.
        assertTrue(labels(catalog, "secondDogCouncil").containsAll(List.of("SpaceAndTime",
                        "MoneyAndVet", "AskZaoHimself")),
                "the council should reuse the voting demo's assessors");

        // The supervisor adds exactly one agent — the nurse, who makes the hand-off reliable —
        // and reuses the routing demo's three. Anything more and the "same cast, different
        // decider" point stops being true.
        var extra = labels(catalog, "supervisor").stream()
                .filter(l -> !l.equals("Supervisor"))
                .filter(l -> !List.of("EverydayCare", "DogTrainer", "EmergencyVet").contains(l))
                .toList();
        assertEquals(List.of("TriageNurse"), extra,
                "the supervisor should add only the nurse: " + labels(catalog, "supervisor"));
    }

    /** The agent names a pattern's diagram shows, which are the agents it is wired from. */
    private static List<String> labels(PatternCatalog c, String id) {
        return c.byId(id).orElseThrow().topology().nodes().stream()
                .map(Topology.Node::label).toList();
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

        // The person must not be drawn as an agent. A human-in-the-loop diagram whose middle
        // box looks like the two either side says the model decided, which is the one thing the
        // pattern exists to deny.
        assertEquals("owner", role(catalog, "humanApproval", "human"),
                "the approval step must be drawn as a person, not an agent");

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
        assertTrue(mutual(catalog, "supervisor", "supervisor", "care"),
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
