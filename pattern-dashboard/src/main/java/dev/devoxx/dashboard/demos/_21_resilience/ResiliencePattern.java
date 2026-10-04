package dev.devoxx.dashboard.demos._21_resilience;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Locale;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.SniffFinds;
import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.devoxx.dashboard.demos._02_sequential.SequentialPattern;
import dev.devoxx.dashboard.demos._21_resilience.Keys.Attempts;
import dev.devoxx.dashboard.demos._21_resilience.Keys.FirstAid;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 21</b> — a dropped call retried, and a step that is skipped when there is
 * nothing for it to do.
 */
public final class ResiliencePattern {

    private ResiliencePattern() {
    }

    /** Past this the handler stops retrying and substitutes. See the caveat: RETRY re-enters. */
    static final int MAX_RETRIES = 2;

    static String run(ChatModel model, String input, StreamingListener listener) {
        // Mission 1's Sniff, on a collar radio that drops its first call. Nothing about the agent
        // knows or cares — flakiness is a property of the call, and recovery is a property of the
        // system, which is why neither of them is in the interface.
        var radio = new FlakyModel(model, 1);
        var sniffOnTheRadio = new AgenticServices.AgentConfigurator(agent -> {
            if (agent.agentServiceClass() == SniffFinds.class) {
                agent.agentBuilder().chatModel(radio);
            }
        });
        // The injury is passed only when the report actually mentions one — deciding whether you
        // hold a value is not a job for a model, and making it one would hide what the demo is about.
        var r = CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(BadRadioDay.class, model, sniffOnTheRadio)
                        .rescue(input, mentionsInjury(input) ? input : null));
        int attempts = requireNonNullElse(r.agenticScope().readState(Attempts.class), 0);
        return outcome(r.agenticScope(), radio, attempts);
    }

    private static boolean mentionsInjury(String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        return List.of("thorn", "hurt", "bleed", "cut", "limp", "scrape", "sting")
                .stream().anyMatch(lower::contains);
    }

    /**
     * Says what actually happened, because both recoveries are invisible in the answer itself: a
     * rescue after a retry looks exactly like one that never failed, and a skipped first-aid step
     * looks exactly like a kitten nobody bothered to check.
     */
    private static String outcome(AgenticScope scope, FlakyModel radio, int attempts) {
        String aid = scope.readState(FirstAid.class);
        return String.valueOf(scope.readState(RescueStatus.class))
                + "\n\n**First aid**\n\n"
                + (aid == null ? "*skipped — nobody hurt in the report, and the step that reads "
                        + "it is optional*" : aid)
                + "\n\n---\n\n*"
                + (attempts == 0 ? "No call dropped." : attempts + " dropped call(s), recovered by retry")
                + " · Sniff's radio was used " + radio.calls() + " times.*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Both recoveries are on the boxes rather than in the edges, because neither is a route
        // through the graph: a retry re-enters the same step and a skip removes one. Drawing
        // either as an arrow would invent a path that no run ever takes.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0),
                        node("sniff", "Sniff", "agent", 1).withSub("radio drops · retried").as("sniff"),
                        node("doc", "Doc", "agent", 2).withSub("optional · may skip").as("doc"),
                        node("zoom", "Zoom", "agent", 3).withSub("Mission 2").as("zoom"),
                        node("out", "rescueStatus", "join", 4).withSub("always produced")),
                List.of(edge("in", "sniff"),
                        edge("sniff", "doc", "location"),
                        edge("doc", "zoom"),
                        edge("zoom", "out")));
        return new PatternDef("resilience", "Optional Agents & Error Handling", "production",
                "A bad radio day: Sniff's collar keeps cutting out, and the kitten is back up the "
                        + "oak. Neither is a reason not to get it down.",
                "Mission 1's Sniff and Mission 2's Zoom, unchanged — on a radio that fails.",
                "Two different answers to \"this step produced nothing\", and they are not "
                        + "interchangeable. **`optional = true`** is about a missing *input*: Doc is "
                        + "skipped when `Injuries` is absent, and it does nothing at all about "
                        + "failure. **`@ErrorHandler`** is about a failing *call*: it sees every "
                        + "`AgentInvocationException` and picks `retry()`, `result(x)` or "
                        + "`throwException()`. Delete the thorn from the input and Doc vanishes "
                        + "without an error; the dropped call is recovered either way.",
                "`RETRY` re-executes the agent and a second failure comes **straight back to your "
                        + "handler** — so a handler without a counter is an infinite loop, and this "
                        + "one carries one. And an optional step that is skipped leaves its key "
                        + "holding whatever was there before, which is usually null: read it "
                        + "expecting nothing.",
                topo,
                // Mentions an injury, so the optional step runs. Delete "a thorn in its paw" on
                // stage and watch the same run skip Doc and still get the kitten down.
                SequentialPattern.KITTEN + " It has a thorn in its paw.",
                ResiliencePattern::run)
                .gist("Skip a step whose input is missing; retry a call that fails.");
    }
}
