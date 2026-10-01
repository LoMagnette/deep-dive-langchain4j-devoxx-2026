package dev.devoxx.dashboard.demos._01_single;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Message;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.TokenStream;

/**
 * Wiring for the <b>single</b> demo — one call, one job.
 */
public final class SinglePattern {

    private SinglePattern() {
    }

    /** Shared with the sequential demo, which runs the same text through a second agent. */
    public static final String BEAGLE_REPORT =
            "OK OK OK so it was THERE, the squirrel, the grey one, the one with the bit missing "
                    + "off its tail, it came DOWN the big oak by the back fence and went ALONG the "
                    + "top of the fence and STOPPED and LOOKED at me, Zao, it LOOKED at me, and then "
                    + "it went to the bird feeder and ate ALL of it and went back UP the oak. it does "
                    + "this every single day. the cat was on the shed roof the whole time watching "
                    + "and did NOTHING, as usual. I barked. it did not care. it does not care about "
                    + "anything. ANYTHING.";

    static String run(ChatModel model, String input, StreamingListener listener) {
        if (listener.streamingModel() != null) {
            return streamed(listener, input);
        }
        var clerk = AgenticServices.agentBuilder(NoteRetriever.class)
                .chatModel(model)
                .name("NoteRetriever")
                .outputKey(Notes.class)
                .build();

        UntypedAgent app = AgenticServices.sequenceBuilder()
                                          .subAgents(clerk)
                                          .outputKey(Notes.class)
                                          .listener(listener)
                                          .build();
        var r = app.invokeWithAgenticScope(Map.of(new Message().name(), input));

        return String.valueOf(r.result());
    }

    /**
     * The same agent with {@code streamingChatModel} and a {@link TokenStream} return type —
     * that return type is what makes it stream, not the builder. It reaches the screen only
     * because it is the LAST agent: put a step after it and the framework drains it internally.
     */
    private static String streamed(StreamingListener listener, String input) {
        var clerk = AgenticServices.agentBuilder(StreamingNoteRetriever.class)
                .streamingChatModel(listener.streamingModel())
                .name("NoteRetriever")
                .outputKey(Notes.class)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(clerk).outputKey(Notes.class).listener(listener).build();
        Object result = app.invokeWithAgenticScope(Map.of(new Message().name(), input)).result();
        if (!(result instanceof TokenStream stream)) {
            return String.valueOf(result);   // right answer, just not streamed
        }

        var done = new CompletableFuture<String>();
        var text = new StringBuilder();
        stream.onPartialResponse(chunk -> {
                    text.append(chunk);
                    listener.emitToken("NoteRetriever", chunk);
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
            // Whatever arrived is what the page has been showing token by token.
            return text.isEmpty() ? "the stream failed: " + e : text.toString();
        }
    }

    /** A stream that never completes would hold one of four run threads for ever. */
    private static final java.time.Duration STREAM_TIMEOUT = java.time.Duration.ofMinutes(3);

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "Beagle's report", "input"),
                        node("clerk", "NoteRetriever", "agent")
                                .withSub("Golden · fetches facts")),
                List.of(edge("in", "clerk")));
        return new PatternDef("single", "Single Agent", "workflow",
                "Operation Squirrel. It has eaten the bird feeder every day for a month. The "
                        + "Beagle has just come in from the garden, shouting.",
                null,
                "One LLM call wrapped as an agent — the simplest useful unit, doing the job an "
                        + "LLM is genuinely best at: turning what somebody actually said — here, "
                        + "a Beagle in full cry — into a shape a system can use.",
                "No decomposition: one agent struggles with multi-step or long tasks — and watch "
                        + "the Time line, because a model would rather invent \"07:00\" than admit "
                        + "the Beagle only ever said \"every single day\".",
                topo,
                // A real report: all caps, out of order, and one field genuinely absent (the
                // Beagle says "every single day" and never a time), so the room can check
                // whether the agent obeys "write not given" or quietly makes something up.
                BEAGLE_REPORT,
                SinglePattern::run,
                // The only demo that honours the token toggle, so the only one the page offers
                // it on. Streaming is a property of the LAST agent, and every other entry in the
                // catalogue ends on something that is not one.
                true);
    }
}
