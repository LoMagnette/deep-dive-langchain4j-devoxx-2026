package dev.devoxx.dashboard.demos._01_single;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.TokenStream;

/**
 * Wiring for <b>Mission 1</b> — one Ranger, one job, and his own choice of gear.
 */
public final class SinglePattern {

    private SinglePattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        if (listener.streamingModel() != null) {
            return streamed(model, listener, input);
        }
        // The whole topology is on HatSearch's annotations. What is left here is the two things
        // the framework cannot know: which model this run is against, and which run is watching.
        return CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(HatSearch.class, model).find(input));
    }

    /**
     * The same agent with {@code streamingChatModel} and a {@link TokenStream} return type —
     * that return type is what makes it stream, not the builder. It reaches the screen only
     * because it is the LAST agent: put a step after it and the framework drains it internally.
     */
    private static String streamed(ChatModel model, StreamingListener listener, String input) {
        TokenStream stream = CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(StreamingHatSearch.class, model).find(input));

        var done = new CompletableFuture<String>();
        var text = new StringBuilder();
        stream.onPartialResponse(chunk -> {
                    text.append(chunk);
                    listener.emitToken("Sniff", chunk);
                })
                .onCompleteResponse(response -> done.complete(response.aiMessage().text()))
                .onError(done::completeExceptionally)
                .start();   // handlers first: an earlier start() drops the opening tokens
        try {
            return done.get(STREAM_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return text.toString();
        } catch (ExecutionException | TimeoutException e) {
            return text.isEmpty() ? "the stream failed: " + e : text.toString();
        }
    }

    /** A stream that never completes would hold one of four run threads for ever. */
    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(3);

    /** Shared with Mission 21, whose Sniff goes looking for the same hat on a bad radio. */
    public static final String LOST_HAT =
            "Paws up, Rangers! The Mayor has lost his hat — the tall green one with the feather. "
                    + "He last had it on the bench in Barkville Park, before the ducks arrived.";

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The gear is drawn as plain-Java boxes the agent MAY reach, with no order between
        // them: the arrows say "it decides", and the page lights each one as the model calls it.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0).withSub("from the Mayor"),
                        node("sniff", "Sniff", "agent", 1).withSub("finds · picks his gear").as("sniff"),
                        node("t1", "sniff(place)", "code", 2).withSub("gear · plain Java"),
                        node("t2", "followTrail(scent)", "code", 2).withSub("gear · plain Java"),
                        node("out", "location", "join", 3).withSub("where the hat is")),
                List.of(edge("in", "sniff"),
                        edge("sniff", "t1", "it decides"), edge("sniff", "t2"),
                        edge("sniff", "out")));
        return new PatternDef("single", "Single Agent", "team",
                "Paws up, Rangers! The Mayor has lost his hat. Sniff goes alone, and decides "
                        + "for himself where to put his nose.",
                null,
                "One `@Agent` interface with tools — the simplest useful unit. The interface hands "
                        + "Sniff his gear (a `@ToolsSupplier` returning `new SniffGear()`) and says "
                        + "nothing about using it: **the model chooses which tool to call, with what argument, "
                        + "and in what order**. Watch the Run events pane — every `tool-call` "
                        + "line is a decision the LLM made, not your code.",
                "The model may call a tool you did not expect, or none at all — and a tool that "
                        + "returns a plausible string is believed. Tools are where facts come "
                        + "from, so make them return facts, and log every call: the tool lines "
                        + "in the Server log are the audit trail.",
                topo,
                LOST_HAT,
                SinglePattern::run,
                // The only demo that honours the token toggle: streaming is a property of the
                // LAST agent, and every other entry ends on something that is not one.
                true)
                .gist("One agent that chooses its own tools.");
    }
}
