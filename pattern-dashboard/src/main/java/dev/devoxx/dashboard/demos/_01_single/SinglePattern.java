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
    public static final String SITTER_MESSAGE =
            "hey!! ok so — zao, the big grey hairy one. he is a bouvier. he is not a bear and "
                    + "he is not a sheep, people ask. "
                    + "food's in the tub by the back door, two scoops morning and evening. he "
                    + "CANNOT have the dried liver treats any more, they go straight through him "
                    + "and you will know about it. do NOT let him off the lead in the park. he "
                    + "does not come back. he has never come back. vet's 061 22 33 44. he will "
                    + "scream the first night like you are taking him apart — ignore it, he's "
                    + "fine, he does it to us too. thank you!!! x";

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
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
                .subAgents(clerk).outputKey(Notes.class).listener(listener).build();
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
                        node("clerk", "NoteRetriever", "agent")),
                List.of(edge("in", "clerk")));
        return new PatternDef("single", "Single Agent", "workflow",
                "You are away this weekend. A friend said yes to having Zao before reading "
                        + "the message. You have just sent them the message.",
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
                SITTER_MESSAGE,
                SinglePattern::run,
                // The only demo that honours the token toggle, so the only one the page offers
                // it on. Streaming is a property of the LAST agent, and every other entry in the
                // catalogue ends on something that is not one.
                true);
    }
}
