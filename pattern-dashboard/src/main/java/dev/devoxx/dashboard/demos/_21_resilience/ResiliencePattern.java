package dev.devoxx.dashboard.demos._21_resilience;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._21_resilience.Keys.MedNote;
import dev.devoxx.dashboard.demos._21_resilience.Keys.Meds;
import dev.devoxx.dashboard.demos._02_sequential.FridgeChecklist;
import dev.devoxx.dashboard.demos._01_single.Keys.Message;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._01_single.SitterCardClerk;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.agent.ErrorRecoveryResult;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>optional agents and error handling</b> demo — the note reaches the fridge
 * door whether or not every step managed to produce something.
 */
public final class ResiliencePattern {

    private ResiliencePattern() {
    }

    /** Past this the handler stops retrying and substitutes. See the caveat: RETRY re-enters. */
    private static final int MAX_RETRIES = 2;

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // Demo 1's clerk, on a connection that drops its first call. Nothing about the agent
        // knows or cares — flakiness is a property of the call, and recovery is a property of
        // the system, which is why neither of them is in the interface.
        var flaky = new FlakyModel(model, 1);
        var clerk = AgenticServices.agentBuilder(SitterCardClerk.class)
                .chatModel(flaky)
                .name("SitterCardClerk")
                .outputKey(Notes.class)
                .build();
        var meds = AgenticServices.agentBuilder(MedicationNote.class)
                .chatModel(model)
                .name("MedicationNote")
                .outputKey(MedNote.class)
                // Skipped when 'meds' is not in the scope. Take this line away and a dog who is
                // on nothing costs you the whole note, with a MissingArgumentException naming a
                // step that looks unrelated.
                .optional(true)
                .build();
        var list = AgenticServices.agentBuilder(FridgeChecklist.class)
                .chatModel(model)
                .name("FridgeChecklist")
                .outputKey(Notes.class)
                .build();

        // The counter is not decoration. RETRY re-executes the agent and a second failure comes
        // straight back here, so a handler that always retries never terminates.
        AtomicInteger attempts = new AtomicInteger();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(clerk, meds, list)
                .errorHandler(ctx -> attempts.incrementAndGet() <= MAX_RETRIES
                        ? ErrorRecoveryResult.retry()
                        : ErrorRecoveryResult.result(
                                "(could not be written — ring us on 061 22 33 44)"))
                .output(scope -> note(scope, flaky, attempts.get()))
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(seed(input));
        return String.valueOf(r.result());
    }

    /**
     * The scope a real app would have: the medication details are there only when the owner
     * actually gave you some. This is plain Java on purpose — deciding whether you hold a value
     * is not a job for a model, and making it one would hide the thing the demo is about.
     */
    private static Map<String, Object> seed(String input) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(new Message().name(), input);
        if (mentionsMedication(input)) {
            state.put(new Meds().name(), input);
        }
        return state;
    }

    private static boolean mentionsMedication(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return lower.contains("tablet") || lower.contains("medication")
                || lower.contains("pill") || lower.contains("capsule");
    }

    /**
     * Says what actually happened, because both recoveries are invisible in the answer itself.
     * A note that came back after a retry looks exactly like one that never failed, and a run
     * that skipped the medication step looks exactly like a dog who is not on anything.
     */
    private static String note(AgenticScope scope, FlakyModel flaky, int attempts) {
        String med = scope.readState(MedNote.class);
        return String.valueOf(scope.readState(Notes.class))
                + "\n\n**Medication**\n\n"
                + (med == null ? "*skipped — no medication in the message, and the step that "
                        + "reads it is optional*" : med)
                + "\n\n---\n\n*"
                + (attempts == 0 ? "No step failed." : attempts + " failure(s), recovered by retry")
                + " · the clerk's model was called " + flaky.calls() + " times for one answer.*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Both recoveries are on the boxes rather than in the edges, because neither of them is
        // a route through the graph: a retry re-enters the same step and a skip removes one.
        // Drawing either as an arrow would invent a path that no run ever takes.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "message", "input", 0),
                        node("clerk", "SitterCardClerk", "agent", 1)
                                .withSub("drops one · retried"),
                        node("meds", "MedicationNote", "agent", 2)
                                .withSub("optional · may skip"),
                        node("list", "FridgeChecklist", "agent", 3),
                        node("out", "the note", "join", 4).withSub("always produced")),
                List.of(edge("in", "clerk"),
                        edge("clerk", "meds", "notes"),
                        edge("meds", "list"),
                        edge("list", "out")));
        return new PatternDef("resilience", "Optional Agents & Error Handling", "production",
                "Most dogs are not on tablets, and the practice's line drops. Neither is a "
                        + "reason for the sitter to end up with no note.",
                "Demo 1's SitterCardClerk and demo 2's FridgeChecklist, unchanged — on a "
                        + "connection that fails.",
                "Two different answers to \"this step produced nothing\", and they are not "
                        + "interchangeable. **`optional(true)`** is about a missing *input*: the "
                        + "step is skipped when a key it declares is absent, and it does nothing "
                        + "at all about failure. **`errorHandler(...)`** is about a failing "
                        + "*call*: it sees every `AgentInvocationException` in the run and picks "
                        + "`retry()`, `result(x)` or `throwException()`. Delete the tablets from "
                        + "the input and the medication step vanishes without an error; the "
                        + "dropped connection is recovered either way.",
                "`RETRY` re-executes the agent and a second failure comes **straight back to your "
                        + "handler** — so a handler without a counter is an infinite loop, and "
                        + "this one carries one. And an optional step that is skipped leaves its "
                        + "key holding whatever was there before, which is usually null: read it "
                        + "expecting nothing.",
                topo,
                // Mentions tablets, so the optional step runs. Delete that sentence on stage and
                // watch the same run skip it and still put a note on the door.
                "we're away Friday to Sunday and my sister is having Zao. Two scoops morning and "
                        + "evening, food in the tub by the back door. He has half a tablet with "
                        + "his breakfast for his hip. Vet is 061 22 33 44.",
                ResiliencePattern::run);
    }
}
