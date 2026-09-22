package dev.devoxx.dashboard.run;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import dev.devoxx.dashboard.run.RunEvent.ScopeValue;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

/**
 * Bridges LangChain4j agentic observability callbacks into {@link RunEvent}s pushed to a sink.
 * Inherited by sub-agents so every agent invocation in a composite is observed.
 */
public class StreamingListener implements AgentListener {

    private final Consumer<RunEvent> sink;
    private final AtomicLong seq;
    private final AskHuman human;
    /** Null unless the web layer supplied them; {@link #tiers(ChatModel)} fills in. */
    private final ModelTiers tiers;
    /** Null unless this run was asked to stream. See {@link #streamingModel()}. */
    private final StreamingChatModel streaming;
    /**
     * When each in-flight invocation started, so an {@code agent-after} can say how long it took.
     */
    private final Map<String, Long> startedNanos = new ConcurrentHashMap<>();

    /** The ordinary run: events out, nobody to ask, one model, no streaming. */
    public StreamingListener(Consumer<RunEvent> sink, AtomicLong seq) {
        this(sink, seq, AskHuman.NOBODY, null, null);
    }

    /** With a person on the other end — demo 7, and the tests' stand-in for one. */
    public StreamingListener(Consumer<RunEvent> sink, AtomicLong seq, AskHuman human) {
        this(sink, seq, human, null, null);
    }

    /** With two model tiers to choose between — the model-routing demo. */
    public StreamingListener(Consumer<RunEvent> sink, AtomicLong seq, AskHuman human,
                             ModelTiers tiers) {
        this(sink, seq, human, tiers, null);
    }

    /**
     * Everything a run may carry. The three shorter forms above delegate here rather than to each
     * other: a four-deep chain of {@code this(..., null)} makes you read every one of them to
     * find out what a run actually gets, which for the class every demo hands to its builder is
     * the wrong thing to make someone do.
     */
    public StreamingListener(Consumer<RunEvent> sink, AtomicLong seq, AskHuman human,
                             ModelTiers tiers, StreamingChatModel streaming) {
        this.sink = sink;
        this.seq = seq;
        this.human = human == null ? AskHuman.NOBODY : human;
        this.tiers = tiers;
        this.streaming = streaming;
    }

    /**
     * The streaming model for this run, or null when the viewer did not ask for one. Null is the
     * signal, not a flag beside it: a demo that can stream branches on having somewhere to
     * stream to, and every other run gets the ordinary path with no extra argument to ignore.
     */
    public StreamingChatModel streamingModel() {
        return streaming;
    }

    /** One chunk of an answer, on its way to the Result pane as it is generated. */
    public void emitToken(String agent, String chunk) {
        emit("token", agent, null, null, chunk);
    }

    /**
     * The two models this run may choose between, falling back to the one it was given for both
     * tiers. The fallback is what lets {@code mvn test} run the model-routing demo with no
     * plumbing at all — and {@link ModelTiers#distinct()} is false there, so nothing claims a
     * choice was made.
     */
    public ModelTiers tiers(ChatModel fallback) {
        return tiers != null ? tiers : ModelTiers.single(fallback, "the live model");
    }

    /**
     * Puts a question to whoever is watching this run and blocks until they answer.
     */
    public String askHuman(String agent, String question) {
        emit("human-ask", agent, question, null, null);
        long from = System.nanoTime();
        String answer = human.ask(question);
        // Worth timing too, and worth showing: it is the honest cost of putting a person in the
        // loop, and it is always the largest number on the page.
        emit("human-answer", agent, answer, null, null, (System.nanoTime() - from) / 1_000_000);
        return answer;
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    @Override
    public void beforeAgentInvocation(AgentRequest r) {
        startedNanos.put(r.agentId(), System.nanoTime());
        emit("agent-before", r.agentName(), "invoking " + r.agentName(), r.agenticScope(), null);
    }

    @Override
    public void afterAgentInvocation(AgentResponse r) {
        Object out = r.output();
        Long took = elapsed(r.agentId());
        emit("agent-after", r.agentName(),
                "completed " + r.agentName() + (took == null ? "" : " in " + took + " ms"),
                r.agenticScope(), truncate(out), took);
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError e) {
        // The whole cause chain, not just getMessage(): see Errors.
        String msg = e.error() == null ? "error" : Errors.explain(e.error());
        emit("agent-error", e.agentName(), "error in " + e.agentName() + ": " + msg,
                e.agenticScope(), null, elapsed(e.agentId()));
    }

    /** Milliseconds since this invocation started, or null if we never saw it start. */
    private Long elapsed(String agentId) {
        Long from = startedNanos.remove(agentId);
        return from == null ? null : (System.nanoTime() - from) / 1_000_000;
    }

    /** Manually push an error event (used when a pattern run throws). */
    public void emitError(String agent, String message) {
        emit("agent-error", agent, message, null, null);
    }

    private void emit(String type, String agent, String message, AgenticScope scope, Object data) {
        emit(type, agent, message, scope, data, null);
    }

    private void emit(String type, String agent, String message, AgenticScope scope, Object data,
                      Long millis) {
        sink.accept(RunEvent.of(seq.getAndIncrement(), type, agent, message, snapshot(scope),
                data, millis));
    }

    /**
     * Snapshot scope state into a small, JSON-safe map (values stringified and truncated).
     * Keys prefixed {@code __} are LangChain4j's internal planner bookkeeping
     * ({@code __planner_state_*}) — they are noise in a panel meant to teach shared state.
     */
    static Map<String, ScopeValue> snapshot(AgenticScope scope) {
        Map<String, ScopeValue> out = new LinkedHashMap<>();
        if (scope == null) {
            return out;
        }
        try {
            for (Map.Entry<String, Object> e : scope.state().entrySet()) {
                if (e.getKey().startsWith("__")) {
                    continue;
                }
                out.put(e.getKey(), describe(e.getValue()));
            }
        } catch (Exception ignore) {
            // scope may be in an inconsistent state during teardown; ignore.
        }
        return out;
    }

    /** Name the type the way a reader would, not the way the JDK does. */
    static ScopeValue describe(Object value) {
        if (value == null) {
            return new ScopeValue("null", null, "null");
        }
        String type;
        Integer size;
        // List.of(...) is really an ImmutableCollections$ListN; "List(3)" is what a person wants.
        if (value instanceof Collection<?> c) {
            type = value instanceof Set ? "Set" : "List";
            size = c.size();
        } else if (value instanceof Map<?, ?> m) {
            type = "Map";
            size = m.size();
        } else if (value instanceof CharSequence s) {
            type = "String";
            size = s.length();
        } else {
            String simple = value.getClass().getSimpleName();
            type = simple.isEmpty() ? value.getClass().getName() : simple;
            size = null;
        }
        return new ScopeValue(type, size, truncate(value));
    }

    static String truncate(Object value) {
        String s = String.valueOf(value);
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
