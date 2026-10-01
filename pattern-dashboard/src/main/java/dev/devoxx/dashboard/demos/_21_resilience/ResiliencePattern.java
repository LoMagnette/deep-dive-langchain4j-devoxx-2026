package dev.devoxx.dashboard.demos._21_resilience;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._01_single.NoteRetriever;
import dev.devoxx.dashboard.demos._02_sequential.BattlePlanner;
import dev.devoxx.dashboard.demos._21_resilience.Keys.FirstAid;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
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
        var clerk = AgenticServices.agentBuilder(NoteRetriever.class)
                .chatModel(flaky)
                .name("NoteRetriever")
                .outputKey(Notes.class)
                .build();
        var firstAid = AgenticServices.agentBuilder(FirstAidNote.class)
                .chatModel(model)
                .name("FirstAidNote")
                .outputKey(FirstAid.class)
                // Skipped when 'Injuries' is not in the scope. Take this line away and an
                // operation where nobody got hurt costs you the whole plan, with a
                // MissingArgumentException naming a step that looks unrelated.
                .optional(true)
                .build();
        var plan = AgenticServices.agentBuilder(BattlePlanner.class)
                .chatModel(model)
                .name("BattlePlanner")
                .outputKey(Notes.class)
                .build();

        // The counter is not decoration. RETRY re-executes the agent and a second failure comes
        // straight back here, so a handler that always retries never terminates.
        AtomicInteger attempts = new AtomicInteger();
        ReportPipeline app = AgenticServices.sequenceBuilder(ReportPipeline.class)
                .name("Sequential")
                .subAgents(clerk, firstAid, plan)
                .errorHandler(ctx -> attempts.incrementAndGet() <= MAX_RETRIES
                        ? ErrorRecoveryResult.retry()
                        : ErrorRecoveryResult.result(
                                "(the report was lost — ask the Beagle again)"))
                .output(scope -> note(scope, flaky, attempts.get()))
                .listener(listener)
                .build();
        // The injury is passed only when the report actually mentions one — deciding whether
        // you hold a value is not a job for a model, and making it one would hide the thing
        // the demo is about.
        return app.write(input, mentionsInjury(input) ? input : null);
    }

    private static boolean mentionsInjury(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return lower.contains("scrape") || lower.contains("bleed") || lower.contains("cut")
                || lower.contains("hurt") || lower.contains("limp") || lower.contains("sting");
    }

    /**
     * Says what actually happened, because both recoveries are invisible in the answer itself.
     * A plan that came back after a retry looks exactly like one that never failed, and a run
     * that skipped the first-aid step looks exactly like one where nobody got hurt.
     */
    private static String note(AgenticScope scope, FlakyModel flaky, int attempts) {
        String aid = scope.readState(FirstAid.class);
        return String.valueOf(scope.readState(Notes.class))
                + "\n\n**First aid**\n\n"
                + (aid == null ? "*skipped — nobody hurt in the report, and the step that "
                        + "reads it is optional*" : aid)
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
                List.of(node("in", "Beagle's report", "input", 0),
                        node("clerk", "NoteRetriever", "agent", 1)
                                .withSub("Golden · fails · retried"),
                        node("meds", "FirstAidNote", "agent", 2)
                                .withSub("optional · may skip"),
                        node("list", "BattlePlanner", "agent", 3).withSub("Collie · the plan"),
                        node("out", "the plan", "join", 4).withSub("always produced")),
                List.of(edge("in", "clerk"),
                        edge("clerk", "meds", "notes"),
                        edge("meds", "list"),
                        edge("list", "out")));
        return new PatternDef("resilience", "Optional Agents & Error Handling", "production",
                "Most operations end with nobody hurt, and the Golden's first go gets lost. "
                        + "Neither is a reason for the back door to have no plan.",
                "Demo 1's NoteRetriever and demo 2's BattlePlanner, unchanged — on a "
                        + "connection that fails.",
                "Two different answers to \"this step produced nothing\", and they are not "
                        + "interchangeable. **`optional(true)`** is about a missing *input*: the "
                        + "step is skipped when a key it declares is absent, and it does nothing "
                        + "at all about failure. **`errorHandler(...)`** is about a failing "
                        + "*call*: it sees every `AgentInvocationException` in the run and picks "
                        + "`retry()`, `result(x)` or `throwException()`. Delete the Corgi's nose "
                        + "from the input and the first-aid step vanishes without an error; the "
                        + "dropped connection is recovered either way.",
                "`RETRY` re-executes the agent and a second failure comes **straight back to your "
                        + "handler** — so a handler without a counter is an infinite loop, and "
                        + "this one carries one. And an optional step that is skipped leaves its "
                        + "key holding whatever was there before, which is usually null: read it "
                        + "expecting nothing.",
                topo,
                // Mentions an injury, so the optional step runs. Delete that sentence on stage and
                // watch the same run skip it and still put a plan on the back door.
                "it got away AGAIN. down the oak, along the top of the fence, ate the bird feeder, "
                        + "back up the oak, and the cat watched the whole thing from the shed roof. "
                        + "and the Corgi scraped his nose on the fence going after it and it is "
                        + "bleeding a bit.",
                ResiliencePattern::run);
    }
}
