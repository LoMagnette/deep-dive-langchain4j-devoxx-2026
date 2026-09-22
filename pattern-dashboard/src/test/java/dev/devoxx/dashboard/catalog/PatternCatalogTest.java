package dev.devoxx.dashboard.catalog;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.devoxx.dashboard.demos.blackboard.HomeNotes;
import dev.devoxx.dashboard.demos.blackboard.RoutineNotes;
import dev.devoxx.dashboard.demos.blackboard.TrainerLead;
import dev.devoxx.dashboard.demos.blackboard.WalkNotes;
import dev.devoxx.dashboard.model.MockChatModel;
import dev.devoxx.dashboard.model.MockStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.devoxx.dashboard.run.AskHuman;
import dev.devoxx.dashboard.run.ModelTiers;
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
     * Same, with two distinguishable models. Only the model-routing demo reads them, and it is
     * the only way to assert which tier it picked: with one model the two tiers are the same
     * object and every run reports the same name.
     */
    private static Run run(PatternDef def, String input, ModelTiers tiers) {
        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        var listener = new StreamingListener(events::add, new AtomicLong(), AskHuman.NOBODY,
                tiers);
        return new Run(def.run(new MockChatModel(), input, listener), events);
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
            // One demo is ABOUT a failing call, so an error event there is the subject rather
            // than a broken demo. The exemption is deliberately narrow: only that pattern, and
            // only on the step it breaks on purpose — anything else erroring is still a bug,
            // and the result assertion below still has to hold for it like everything else.
            r.errors().stream()
                    .filter(e -> !("resilience".equals(info.id()) && e.contains("SitterCardClerk")))
                    .forEach(e -> failures.add(info.id() + " -> " + e));
            if (r.result() == null || r.result().isBlank() || "null".equals(r.result())) {
                // The old conditional-routing bug produced exactly this: no error, no answer.
                failures.add(info.id() + " -> produced no result (" + r.result() + ")");
            }
        }

        assertEquals(19, catalog.infos().stream()
                        .filter(i -> !i.category().equals("composite")).count(),
                "expected all 19 patterns registered");
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

        // The optional step must actually be skipped when its key is absent, and the note must
        // still come out. A run that quietly answers anyway has not demonstrated optional at
        // all — it has demonstrated an agent that ignores its own arguments.
        Run noMeds = run(catalog.byId("resilience").orElseThrow(),
                "away Friday to Sunday, my sister has him, two scoops morning and evening, "
                        + "vet 061 22 33 44");
        assertTrue(noMeds.errors().stream().noneMatch(e -> e.contains("MedicationNote")),
                "a skipped optional step is not an error: " + noMeds.errors());
        assertTrue(!noMeds.invoked().contains("MedicationNote"),
                "with no medication in the message that step must be skipped: "
                        + noMeds.invoked());
        assertTrue(noMeds.result().contains("skipped"),
                "the result must say the step was skipped, or a skip looks like a dog on "
                        + "nothing: " + noMeds.result());
    }

    /** How many times an agent was invoked in this run. Loops and debate rounds repeat names. */
    private static long times(Run r, String agent) {
        return r.invoked().stream().filter(agent::equals).count();
    }

    /**
     * The debate's claim is not that agents argue — it is that the argument <b>ends</b>, and ends
     * for one of two different reasons on the same page. The holiday advocates agree, so
     * {@code ConvergenceStrategy.unanimous()} fires after round one; the council's do not, so that
     * debate runs its full two rounds and the judge is called on a genuine disagreement.
     *
     * <p>Nothing pinned either behaviour before, and both are one mock rule away from vanishing:
     * the holiday advocates converge because they fall through to a catch-all that hands both
     * sides the same words, so any new rule inserted between the judge's rule and that catch-all
     * turns the holiday debate into a second copy of the council's. It would still pass the smoke
     * test, still draw the same diagram, and quietly stop showing the contrast.
     */
    @Test
    void theDebateConvergesOnAgreementAndNotOtherwise() {
        var catalog = new PatternCatalog();

        Run holiday = run(catalog.byId("debate").orElseThrow());
        assertTrue(holiday.errors().isEmpty(), holiday.errors()::toString);
        assertEquals(1, times(holiday, "TakeHimAdvocate"),
                "both advocates said the same thing, so the debate must stop after ONE round: "
                        + holiday.invoked());
        assertEquals(1, times(holiday, "LeaveHimAdvocate"),
                "both advocates said the same thing, so the debate must stop after ONE round: "
                        + holiday.invoked());
        assertEquals(1, times(holiday, "HolidayVerdict"),
                "the judge rules once, on the round that converged: " + holiday.invoked());

        Run council = run(catalog.byId("secondDogCouncil").orElseThrow());
        assertTrue(council.errors().isEmpty(), council.errors()::toString);
        assertEquals(2, times(council, "SecondDogFor"),
                "the council's advocates disagree, so this debate must run its full two rounds "
                        + "— the opposite behaviour, on the same page: " + council.invoked());
        assertEquals(2, times(council, "SecondDogAgainst"),
                "the council's advocates disagree, so this debate must run its full two rounds: "
                        + council.invoked());
        assertEquals(1, times(council, "HouseholdVerdict"),
                "the judge is called once, after the rounds run out: " + council.invoked());
    }

    /**
     * P2P's claim is the one its diagram spends a box on: the run ends because the exit predicate
     * fired, not because it hit the round cap. Neither peer can overrule the other, so without a
     * predicate they counter each other until {@code P2PPlanner}'s limit of ten rounds — which
     * looks identical from the outside unless you count the invocations.
     */
    @Test
    void theTwoPeersSettleOnThePredicateRatherThanRunningOutOfRounds() {
        Run r = run(new PatternCatalog().byId("p2p").orElseThrow());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        // The peers only: the planner wrapper reports itself too, under its method name
        // ("invoke"), because plannerBuilder() takes no .name(). That is the same default the
        // .name("X") rule is about, one layer up.
        var peers = r.invoked().stream()
                .filter(a -> a.equals("TeamOnTheBed") || a.equals("TeamOnTheFloor")).toList();
        assertEquals(List.of("TeamOnTheBed", "TeamOnTheFloor"), peers,
                "one exchange settles it: the floor answers with an agreement, the predicate "
                        + "sees it and the run stops. More invocations than this means the "
                        + "predicate stopped firing and the peers are countering each other to "
                        + "the ten-round cap: " + r.invoked());
        assertTrue(r.result() != null && !r.result().isBlank() && !"null".equals(r.result()),
                "the agreement is the result — the key the predicate waits for: " + r.result());
    }

    /**
     * The blackboard's claim is that <b>any contributor can go first</b>, which is what separates
     * it from the sequence it used to be. That is a property of the agents' declared inputs, not
     * of one run: each note-taker reads only {@code problem}, so nothing orders them; the lead
     * reads all three, so it can only go last. Asserted from the interfaces for exactly that
     * reason — a run shows one order, and one order is what a sequence shows too.
     */
    @Test
    void anyBlackboardContributorCouldGoFirstAndOnlyTheLeadCanGoLast() {
        for (Class<?> notes : List.of(WalkNotes.class, RoutineNotes.class, HomeNotes.class)) {
            assertEquals(List.of("Problem"), inputKeys(notes),
                    notes.getSimpleName() + " must read ONLY the Problem. Give it a key another "
                            + "contributor writes and the board has an order again, which is the "
                            + "sequence this demo was rewritten to stop being.");
        }
        assertEquals(List.of("Walks", "Routine", "Home"), inputKeys(TrainerLead.class),
                "the lead reads the whole board, which is what makes it the step that ends the run");

        Run r = run(new PatternCatalog().byId("blackboard").orElseThrow());
        assertTrue(r.errors().isEmpty(), r.errors()::toString);
        assertTrue(r.invoked().containsAll(List.of("WalkNotes", "RoutineNotes", "HomeNotes")),
                "every angle must reach the board: " + r.invoked());
        assertEquals("TrainerLead", r.invoked().get(r.invoked().size() - 1),
                "the lead needs all three, so it can only run once they have: " + r.invoked());
    }

    /**
     * The scope keys an agent interface declares as inputs, in declaration order — resolved the
     * way the framework resolves them, so this reads whichever annotation the agent used.
     * {@code @K(Notes.class)} is the typed form and names the key {@code TypedKey.name()}
     * returns; {@code @V("food")} survives only where there is no key to point at, which is the
     * parallel mapper's item.
     */
    private static List<String> inputKeys(Class<?> agent) {
        var method = java.util.Arrays.stream(agent.getMethods())
                .filter(m -> m.isAnnotationPresent(dev.langchain4j.agentic.Agent.class))
                .findFirst().orElseThrow(() -> new AssertionError(
                        agent.getSimpleName() + " has no @Agent method"));
        return java.util.Arrays.stream(method.getParameters())
                .map(PatternCatalogTest::declaredKey)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /** The key one parameter binds to, from {@code @K} or {@code @V}, or null if it binds none. */
    private static String declaredKey(java.lang.reflect.Parameter p) {
        var typed = p.getAnnotation(dev.langchain4j.agentic.declarative.K.class);
        if (typed != null) {
            try {
                return typed.value().getDeclaredConstructor().newInstance().name();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("a TypedKey must be a no-args record: " + typed.value(), e);
            }
        }
        var named = p.getAnnotation(dev.langchain4j.service.V.class);
        return named == null ? null : named.value();
    }

    /**
     * A non-AI agent's claim is that the framework cannot tell it apart from an LLM one, and the
     * only honest way to assert that is from the outside: the two Java steps must appear in the
     * run exactly like the model step does — invoked, timed, and writing to the scope.
     *
     * <p>The second half is the reason the demo exists at all. The canned note deliberately
     * drops two of the record's numbers, so the guard has something to catch; if a future prompt
     * change made the model copy everything, this demo would silently become ceremony and the
     * assertion below is what would say so.
     */
    @Test
    void theJavaStepsAreIndistinguishableFromTheModelStepAndActuallyDoTheWork() {
        var def = new PatternCatalog().byId("nonAiAgent").orElseThrow();
        Run r = run(def);
        assertTrue(r.errors().isEmpty(), r.errors()::toString);

        // Both Java steps genuinely ran, and neither was reported to the listener. That second
        // half is a LIBRARY GAP, not a choice: NonAiAgentInstance.setParent sets the parent and
        // never calls registerInheritedParentListener, which AgentInvocationHandler and
        // PlannerBasedInvocationHandler both do. So a non-AI agent inherits no listener, emits
        // no events and is never timed.
        //
        // Pinned rather than worked around, because the demo's caveat states it as fact and
        // because it is exactly the kind of thing a version bump fixes quietly. If this line
        // goes red on an upgrade, the library fixed it: delete the assertion, and rewrite the
        // caveat in NonAiAgentPattern.define() — it will have become wrong.
        assertEquals(List.of("NoteFromFile"),
                r.invoked().stream().filter(a -> !a.equals("Sequential")).toList(),
                "only the LLM step is observable in 1.20.0-beta30 — if the Java steps now "
                        + "appear here the library has been fixed: " + r.invoked());

        // So the proof that the Java steps ran is their EFFECT, not their events. The file's
        // output key reached the scope...
        assertTrue(r.events().stream().anyMatch(e -> e.scope() != null
                        && e.scope().containsKey("Facts")),
                "the non-AI agent must write its output key into the scope");

        // ...and the guard earned its place: the model's note left two numbers out, and the run
        // still ends with them on the page.
        assertTrue(r.result().contains("981098106123456")
                        && r.result().contains("AG-4471209"),
                "the guard must put back what the note left out: " + r.result());
        assertTrue(r.result().contains("left out of the note above"),
                "a guard that never fires proves nothing: " + r.result());
    }

    /**
     * Dynamic model selection has one claim and the answer text cannot carry it: the same agent
     * with the same prompt produces an answer that looks identical whichever model ran it. So
     * the test supplies two distinguishable tiers and asserts on which one was chosen.
     *
     * <p>Without the tiers this run would be honest but vacuous — both tiers resolve to the one
     * model the {@code Runner} was given, and {@code ModelTiers.distinct()} is false.
     */
    @Test
    void theExpensiveModelIsUsedOnlyWhereBeingWrongIsExpensive() {
        var def = new PatternCatalog().byId("modelRouting").orElseThrow();
        var tiers = ModelTiers.of(new MockChatModel(), "tiny", new MockChatModel(), "big");

        Run poisoning = run(def, def.defaultInput(), tiers);
        assertTrue(poisoning.errors().isEmpty(), poisoning.errors()::toString);
        assertTrue(poisoning.result().startsWith("**emergency → big"),
                "a dog that has eaten chocolate must buy the strong model: "
                        + poisoning.result());

        // The saving, which is the entire reason to do this: an ordinary question must NOT
        // reach the expensive tier. This is the assertion that separates the demo from one
        // that always picks the big model and never says so.
        Run kibble = run(def, "which food should I buy for a four-year-old shepherd?", tiers);
        assertTrue(kibble.result().startsWith("**everyday → tiny"),
                "a kibble question must settle on the cheap model: " + kibble.result());

        // And a behaviour question, so the cheap tier is not merely the default for anything
        // the classifier fails to recognise.
        Run pulling = run(def, "he pulls like a train on the lead", tiers);
        assertTrue(pulling.result().startsWith("**training → tiny"),
                "a training question does not need the strong model: " + pulling.result());
    }

    /**
     * The error handler's claim: the run survives a call that fails, and it survives it by
     * retrying rather than by pretending. Both halves matter — a demo that swallows the failure
     * silently looks exactly like one where nothing went wrong.
     */
    @Test
    void theFailingCallIsRetriedAndTheNoteStillReachesTheDoor() {
        var def = new PatternCatalog().byId("resilience").orElseThrow();
        Run r = run(def);

        assertTrue(r.result() != null && !r.result().isBlank(), "the note must survive");
        assertTrue(r.result().contains("061 22 33 44"),
                "the recovered note still has to satisfy the fridge rules: " + r.result());

        // The clerk's model is called twice for one answer: once to fail, once to succeed.
        assertTrue(r.result().contains("called 2 times"),
                "the retry must be visible, or a recovered run looks like a clean one: "
                        + r.result());
        assertTrue(r.result().contains("recovered by retry"),
                "the result must name the recovery: " + r.result());

        // The default input DOES mention a tablet, so the optional step runs here — the mirror
        // of the skip asserted above. Both paths, or the step is only ever tested one way.
        assertTrue(r.invoked().contains("MedicationNote"),
                "with a tablet in the message the optional step must run: " + r.invoked());
    }

    /**
     * The streaming toggle's claim is a negative one, and it is the only thing worth asserting:
     * turning it on changes <b>how the answer arrives and nothing else</b>. The two runs use
     * different agent interfaces and different model types, so "the answers are identical" is a
     * real result rather than a tautology — and it is what makes the toggle safe to flip on
     * stage mid-sentence.
     */
    @Test
    void streamingChangesHowTheAnswerArrivesAndNothingElse() {
        var catalog = new PatternCatalog();
        var def = catalog.byId("single").orElseThrow();
        assertTrue(def.streams(), "demo 1 is the one that offers the toggle");

        // Only the last agent of a run can stream to a screen, and demo 1 is the only entry
        // that ends on one. A toggle offered where it silently does nothing is worse than none.
        assertEquals(List.of("single"), catalog.infos().stream()
                        .filter(PatternDef.PatternInfo::streams)
                        .map(PatternDef.PatternInfo::id).toList(),
                "exactly one demo may advertise streaming");

        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        var listener = new StreamingListener(events::add, new AtomicLong(), AskHuman.NOBODY,
                null, new MockStreamingChatModel());
        String streamed = def.run(new MockChatModel(), def.defaultInput(), listener);

        List<RunEvent> tokens = events.stream()
                .filter(e -> "token".equals(e.type())).toList();
        assertTrue(tokens.size() > 5,
                "the answer has to arrive in pieces, or nothing was demonstrated: "
                        + tokens.size() + " token events");
        assertTrue(tokens.stream().allMatch(e -> e.message() == null),
                "a token carries its chunk in data and has nothing to say in message");

        String reassembled = tokens.stream().map(e -> String.valueOf(e.data()))
                .reduce("", String::concat);
        assertEquals(streamed, reassembled,
                "the tokens the page drew must add up to the answer the run returned");
        assertEquals(run(def).result(), streamed,
                "streaming must change the delivery and not the answer");
    }

    /**
     * The async claim, measured the same way the parallel one is: against a model where every
     * call costs the same, a sequence carrying one async step finishes in materially less than
     * the time its agents spent. The Sequential step's own duration is the library's
     * measurement of the whole thing, which is a better number than one taken out here.
     */
    @Test
    void theAsyncStepOverlapsTheStepsDeclaredAfterIt() {
        var def = new PatternCatalog().byId("async").orElseThrow();
        long delay = 200;
        List<RunEvent> events = Collections.synchronizedList(new ArrayList<>());
        def.run(slowModel(delay), def.defaultInput(),
                new StreamingListener(events::add, new AtomicLong()));

        List<RunEvent> done = events.stream()
                .filter(e -> "agent-after".equals(e.type())).toList();
        long agentTime = done.stream()
                .filter(e -> List.of("VetCallback", "MealPlanner", "WalkPlanner")
                        .contains(e.agent()))
                .mapToLong(RunEvent::millis).sum();
        long step = done.stream().filter(e -> "Sequential".equals(e.agent()))
                .mapToLong(RunEvent::millis).max().orElseThrow();

        assertEquals(3, done.stream().filter(e -> List.of("VetCallback", "MealPlanner",
                        "WalkPlanner").contains(e.agent())).count(),
                "all three steps must run: " + done.stream().map(RunEvent::agent).toList());
        assertTrue(step < agentTime * 0.8,
                "the async step must overlap the ones after it: the sequence took " + step
                        + "ms against " + agentTime + "ms of agent time");
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
     *
     * <p>It covers <b>both ends</b> of the contract. The output side is the builder
     * ({@code outputKey("x")}); the input side is the parameter, where {@code @V("x")} is what
     * every LangChain4j example on the internet uses and {@code @K(Xxx.class)} is what this repo
     * uses. A {@code @V} relapse does at least fail loudly at run time — the prompt template
     * refuses an unknown variable — but it puts a key back in a string, which is the habit this
     * whole mechanism exists to break.
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

        // The input side, over every file in the demos. The two exceptions are the parallel
        // mapper's item: MapperAgentInvoker injects it into the sub-agent's FIRST ARGUMENT by
        // position, so that parameter names nothing in the scope and has no TypedKey to point
        // at. Anything else naming a key in a string belongs in a Keys record.
        var itemNames = Set.of("food", "angle");
        var stringParam = java.util.regex.Pattern.compile("@V\\(\"(\\w+)\"\\)");
        try (var paths = java.nio.file.Files.walk(demos)) {
            // package-info is prose about the rule, and quotes the form it is telling you not
            // to use.
            for (var p : paths.filter(p -> p.toString().endsWith(".java")
                    && !p.getFileName().toString().equals("package-info.java")).toList()) {
                var m = stringParam.matcher(java.nio.file.Files.readString(p));
                while (m.find()) {
                    if (!itemNames.contains(m.group(1))) {
                        offenders.add(p.getFileName() + " uses @V(\"" + m.group(1)
                                + "\") — use @K(" + m.group(1) + ".class)");
                    }
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
        // Agents only: the input box and the supervisor itself are the diagram's scaffolding,
        // not part of the cast this is counting.
        var extra = nodes(catalog, "supervisor").stream()
                .filter(n -> n.role().equals("agent")).map(Topology.Node::label)
                .filter(l -> !List.of("EverydayCare", "DogTrainer", "EmergencyVet").contains(l))
                .toList();
        assertEquals(List.of("TriageNurse"), extra,
                "the supervisor should add only the nurse: " + labels(catalog, "supervisor"));
    }

    private static List<Topology.Node> nodes(PatternCatalog c, String id) {
        return c.byId(id).orElseThrow().topology().nodes();
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

        // GOAP's picture is otherwise pixel-for-pixel a sequence: same boxes, same arrows,
        // left to right. What distinguishes it is that every box declares the key it needs, so
        // the arrows read as derived rather than typed. Without these the diagram is a lie.
        var goapSubs = nodes(catalog, "goap").stream()
                .filter(n -> n.role().equals("agent")).map(Topology.Node::sub).toList();
        assertEquals(3, goapSubs.stream().filter(x -> x != null && x.contains("needs")).count(),
                "every GOAP agent must show what it needs, or this is just a sequence: "
                        + goapSubs);

        // A mapper is one agent invoked once per item. A single box says "one call".
        assertTrue(nodes(catalog, "parallelMapper").stream().anyMatch(Topology.Node::stacked),
                "the mapped agent must be drawn as a stack");

        // The async step's whole claim is that it SPANS the steps after it. Drawn as a plain
        // chain the picture is demo 2 exactly, and the one thing that differs — that the vet is
        // still working while the planners run — is the thing not on the page. The skip-ahead
        // edge is what says it, and in a stages layout it arcs over the boxes between its ends.
        var spanning = edges(catalog, "async").stream()
                .filter(e -> e.from().equals("vet") && e.to().equals("join")).toList();
        assertEquals(1, spanning.size(), "the async step must reach the join directly");
        assertTrue(spanning.get(0).label() != null && spanning.get(0).label().contains("read"),
                "the long edge has to say that the READ is the join, not the step: "
                        + spanning.get(0).label());
        assertEquals(3, stageOf(catalog, "async", "join") - stageOf(catalog, "async", "vet"),
                "the async edge must skip columns, or it is drawn flat and disappears behind "
                        + "the boxes it passes");

        // Neither recovery is a route through the graph — a retry re-enters the same step and a
        // skip removes one — so both live on the boxes. If they were edges the picture would
        // invent paths no run ever takes; if they were nowhere it would be a plain sequence.
        var resilienceSubs = nodes(catalog, "resilience").stream()
                .map(Topology.Node::sub).filter(java.util.Objects::nonNull).toList();
        assertTrue(resilienceSubs.stream().anyMatch(s -> s.contains("retried")),
                "the diagram must show which step is retried: " + resilienceSubs);
        assertTrue(resilienceSubs.stream().anyMatch(s -> s.contains("optional")),
                "the diagram must show which step may be skipped: " + resilienceSubs);
        assertNull(role(catalog, "resilience", "router"),
                "nothing here routes: an optional step is skipped, not branched around");

        // One desk box, not two. Two boxes both labelled DutyDesk would both light up on a run
        // (nodes are marked by agent name), which would say both models answered.
        assertEquals(1, nodes(catalog, "modelRouting").stream()
                        .filter(n -> "DutyDesk".equals(n.label())).count(),
                "the one agent must be drawn once, or the run marks two boxes for one call");
        assertTrue(nodes(catalog, "modelRouting").stream()
                        .anyMatch(n -> n.sub() != null && n.sub().contains("strong")),
                "the desk box has to say that its model is the variable");

        // The supervisor's nurse is called first and the rest only if she says so; a symmetric
        // star would say all four are equal peers, which is a fan-out.
        assertTrue(nodes(catalog, "supervisor").stream()
                        .anyMatch(n -> "1 · always first".equals(n.sub())),
                "the supervisor diagram must show which call comes first");

        // The person must not be drawn as an agent. A human-in-the-loop diagram whose middle
        // box looks like the two either side says the model decided, which is the one thing the
        // pattern exists to deny.
        assertEquals("owner", role(catalog, "humanApproval", "human"),
                "the approval step must be drawn as a person, not an agent");

        // Same argument one step further: the two Java steps must not be drawn as agents. The
        // framework genuinely cannot tell them apart — that is the lesson — but a picture that
        // cannot either says the model did the database lookup.
        assertEquals(2, nodes(catalog, "nonAiAgent").stream()
                        .filter(n -> "code".equals(n.role())).count(),
                "both plain-Java steps must be drawn as code, not as agents");
        assertEquals(1, nodes(catalog, "nonAiAgent").stream()
                        .filter(n -> "agent".equals(n.role())).count(),
                "exactly one step here is a model, and the picture has to say which");

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
        // The nurse edge specifically: the supervisor invokes her and reads what comes back,
        // which is the edge the whole demo turns on. One-way arrows would draw a fan-out.
        assertTrue(mutual(catalog, "supervisor", "supervisor", "nurse"),
                "the supervisor must be shown reading the nurse's answer, not just calling her");
        assertTrue(mutual(catalog, "blackboard", "walks", "board"),
                "blackboard contributors read as well as write");

        // The ladder's rungs must be in SEPARATE columns. Stacked in one column — which is how
        // this was drawn first — the picture is demo 6's branch diagram: one input arriving at
        // one of three desks, which is the exact reading a cost ladder exists to correct.
        var rungs = nodes(catalog, "customPlanner").stream()
                .filter(n -> n.role().equals("agent")).map(Topology.Node::stage).toList();
        assertEquals(3, Set.copyOf(rungs).size(),
                "each rung needs its own column, or the ladder reads as a branch: " + rungs);

        // Every loop needs BOTH ways out of its critic drawn. With only the backward arc, the
        // picture is two agents circling for ever and the exit condition — the whole of what
        // you have to get right — is the one thing missing.
        assertEquals(1, inDegree(catalog, "loop", role(catalog, "loop", "join")),
                "the loop must draw where it leaves the loop, not only how it goes round");
        assertTrue(edges(catalog, "loop").stream()
                        .anyMatch(e -> e.label() != null && e.label().contains("≥")),
                "the exit edge must say what ends the loop");

        // Peers with no drawn exit say the run never terminates, which is the pattern's caveat
        // rather than its behaviour — this one stops the moment an agreement exists.
        assertNotNull(role(catalog, "p2p", "join"),
                "p2p must draw the exit predicate, or the diagram has no end");

        // Both a debate and a supervisor read backwards without a direction: the mesh circle put
        // the judge to the LEFT of the advocates, and the star drew the supervisor as a wheel
        // with four equal spokes — a picture of the fan-out the demo spends its time denying.
        for (String id : List.of("debate", "supervisor")) {
            assertEquals("stages", catalog.byId(id).orElseThrow().topology().layout(),
                    id + " needs explicit columns; a circle has no before and after");
        }

        // render.js trims a label at 22 characters and a sub-line at 26, silently and with no
        // error — so an over-long one is simply wrong on the projector and nowhere else.
        // 24 rather than 26 for subs: at 10.5px in a 150px box, 26 characters touch both walls.
        for (var info : catalog.infos()) {
            for (var n : info.topology().nodes()) {
                assertTrue(n.label().length() <= 22,
                        () -> info.id() + ": label is cut off on the diagram: " + n.label());
                assertTrue(n.sub() == null || n.sub().length() <= 24,
                        () -> info.id() + ": sub-line is cut off on the diagram: " + n.sub());
            }
        }
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

    /** Which column a node is pinned to in a {@code stages} diagram. */
    private static int stageOf(PatternCatalog c, String id, String node) {
        return nodes(c, id).stream().filter(n -> n.id().equals(node))
                .map(Topology.Node::stage).filter(java.util.Objects::nonNull)
                .findFirst().orElseThrow(() -> new AssertionError(
                        id + ": node '" + node + "' has no stage"));
    }

    private static boolean mutual(PatternCatalog c, String id, String a, String b) {
        var es = edges(c, id);
        return es.stream().anyMatch(e -> e.from().equals(a) && e.to().equals(b))
                && es.stream().anyMatch(e -> e.from().equals(b) && e.to().equals(a));
    }

}
