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
    public static final String HUMAN_MESSAGE =
            "ok pack, listen. I'm away till Sunday and nobody is coming, so Zao is in charge, "
                    + "god help us all. the feeder in the kitchen does breakfast and dinner, two "
                    + "scoops each. Labrador, you get ONE scoop, we have talked about this. NOBODY "
                    + "touches the dried liver treats, they go straight through Zao and you will "
                    + "all know about it. the dog flap is open to the garden. Dachshund: NO "
                    + "digging under the fence, we have talked about that too. vet's 061 22 33 44 "
                    + "if anything happens — I know none of you can use a phone. Zao, put it "
                    + "down. the Greyhound will howl the first night like he's being taken apart, "
                    + "ignore him, he does it to me too. love you all x";

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
                List.of(node("in", "message", "input"),
                        node("clerk", "NoteRetriever", "agent")
                                .withSub("Border Collie · reads")),
                List.of(edge("in", "clerk")));
        return new PatternDef("single", "Single Agent", "workflow",
                "The human is away this weekend and nobody is coming. Zao is in charge. The "
                        + "human has just sent the pack a voice note.",
                null,
                "One LLM call wrapped as an agent — the simplest useful unit, doing the job an "
                        + "LLM is genuinely best at: turning what a human actually typed into a "
                        + "shape a system can use.",
                "No decomposition: one agent struggles with multi-step or long tasks — and watch "
                        + "the Walks line, because a model would rather invent a walk time than "
                        + "admit the message never gave one.",
                topo,
                // A real message: no punctuation, out of order, and one field genuinely absent
                // (nobody said when to walk him), so the room can check whether the agent obeys
                // "write not given" or quietly makes something up.
                HUMAN_MESSAGE,
                SinglePattern::run,
                // The only demo that honours the token toggle, so the only one the page offers
                // it on. Streaming is a property of the LAST agent, and every other entry in the
                // catalogue ends on something that is not one.
                true);
    }
}
