package dev.devoxx.dashboard.demos.single;

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
import dev.devoxx.dashboard.demos.single.Keys.Message;
import dev.devoxx.dashboard.demos.single.Keys.Notes;
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

    /**
     * The message every household sends the friend who is watching the dog. Shared with the
     * sequential demo, which runs the same input through a second agent — seeing the identical
     * text produce a different artefact is the point of putting them side by side.
     */
    public static final String SITTER_MESSAGE =
            "hey so thanks again for having zao!! he's the big black belgian shepherd, food's "
                    + "in the tub by the back door he has two scoops morning and evening, oh and "
                    + "he CANNOT have the dried liver treats anymore they upset him. don't let "
                    + "him off the lead in the park he won't come back yet. vet is 061 22 33 44 "
                    + "if anything happens. he'll cry the first night, ignore it, he's fine!!";

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        if (listener.streamingModel() != null) {
            return streamed(listener, input);
        }
        var clerk = AgenticServices.agentBuilder(SitterCardClerk.class)
                .chatModel(model)
                .name("SitterCardClerk")
                .outputKey(Notes.class)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(clerk).outputKey(Notes.class).listener(listener).build();
        var r = app.invokeWithAgenticScope(Map.of(new Message().name(), input));
        return String.valueOf(r.result());
    }

    /**
     * The same one agent, with {@code streamingChatModel} instead of {@code chatModel} and an
     * interface whose method returns a {@link TokenStream}. Everything else — the builder, the
     * sequence, the key, the listener — is identical, which is the whole content of the toggle.
     *
     * <p>Two things make the stream reach this method rather than being drained inside the
     * framework: the agent's return type is a {@code TokenStream}, and it is the <b>last</b>
     * agent of an {@code UntypedAgent} sequence. Put another step after it and the tokens are
     * consumed internally and the scope gets the finished text instead — which is the right
     * behaviour, and the reason only a final agent can stream to a screen.
     */
    private static String streamed(StreamingListener listener, String input) {
        var clerk = AgenticServices.agentBuilder(StreamingSitterCardClerk.class)
                .streamingChatModel(listener.streamingModel())
                .name("SitterCardClerk")
                .outputKey(Notes.class)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(clerk).outputKey(Notes.class).listener(listener).build();
        Object result = app.invokeWithAgenticScope(Map.of(new Message().name(), input)).result();
        if (!(result instanceof TokenStream stream)) {
            // Not an error worth throwing: the answer is right, it just arrived in one piece.
            return String.valueOf(result);
        }

        var done = new CompletableFuture<String>();
        var text = new StringBuilder();
        stream.onPartialResponse(chunk -> {
                    text.append(chunk);
                    listener.emitToken("SitterCardClerk", chunk);
                })
                .onCompleteResponse(response -> done.complete(response.aiMessage().text()))
                .onError(done::completeExceptionally)
                // Nothing happens until start(): the handlers are registered first, so a stream
                // that began on the line above would drop the tokens sent before this one.
                .start();
        try {
            return done.get(STREAM_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return text.toString();
        } catch (ExecutionException | TimeoutException e) {
            // Whatever arrived is still the honest answer, and on a projector it is the visible
            // one — the page has been showing it token by token.
            return text.isEmpty() ? "the stream failed: " + e : text.toString();
        }
    }

    /** A stream that never completes would hold one of four run threads for ever. */
    private static final java.time.Duration STREAM_TIMEOUT = java.time.Duration.ofMinutes(3);

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "message", "input"),
                        node("clerk", "SitterCardClerk", "agent")),
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
