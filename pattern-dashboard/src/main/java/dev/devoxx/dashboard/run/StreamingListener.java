package dev.devoxx.dashboard.run;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import dev.devoxx.dashboard.run.RunEvent.ScopeValue;
import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Bridges LangChain4j agentic observability callbacks into {@link RunEvent}s pushed to a sink.
 * Inherited by sub-agents so every agent invocation in a composite is observed.
 *
 * <p>It also carries the run's channel to a person ({@link AskHuman}). That lives here rather
 * than being a fourth argument to every {@code Runner} because the listener already <i>is</i> the
 * per-run context object, and only one demo out of seventeen needs to ask anybody anything.
 */
public class StreamingListener implements AgentListener {

    private final Consumer<RunEvent> sink;
    private final java.util.concurrent.atomic.AtomicLong seq;
    private final AskHuman human;
    /**
     * When each in-flight invocation started, so an {@code agent-after} can say how long it took.
     *
     * <p>Keyed by {@code agentId()} rather than the agent's name, because a loop invokes the same
     * name several times and a mapper fans one agent out over every item at once — names repeat,
     * ids do not. Concurrent by necessity: a parallel step calls back from several threads.
     */
    private final java.util.Map<String, Long> startedNanos = new java.util.concurrent.ConcurrentHashMap<>();

    public StreamingListener(Consumer<RunEvent> sink, java.util.concurrent.atomic.AtomicLong seq) {
        this(sink, seq, AskHuman.NOBODY);
    }

    public StreamingListener(Consumer<RunEvent> sink,
                             java.util.concurrent.atomic.AtomicLong seq, AskHuman human) {
        this.sink = sink;
        this.seq = seq;
        this.human = human == null ? AskHuman.NOBODY : human;
    }

    /**
     * Puts a question to whoever is watching this run and blocks until they answer.
     *
     * <p>The {@code human-ask} event is emitted first and the wait happens after, so the page has
     * the question on screen before anything is waiting on it — do it the other way round and the
     * run blocks on a question nobody has been shown.
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
