package dev.devoxx.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import dev.langchain4j.agentic.observability.AgentInvocationError;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Bridges LangChain4j agentic observability callbacks into {@link RunEvent}s pushed to a sink.
 * Inherited by sub-agents so every agent invocation in a composite is observed.
 */
public class StreamingListener implements AgentListener {

    private final Consumer<RunEvent> sink;
    private final java.util.concurrent.atomic.AtomicLong seq;

    public StreamingListener(Consumer<RunEvent> sink, java.util.concurrent.atomic.AtomicLong seq) {
        this.sink = sink;
        this.seq = seq;
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    @Override
    public void beforeAgentInvocation(AgentRequest r) {
        emit("agent-before", r.agentName(), "invoking " + r.agentName(), r.agenticScope(), null);
    }

    @Override
    public void afterAgentInvocation(AgentResponse r) {
        Object out = r.output();
        emit("agent-after", r.agentName(), "completed " + r.agentName(), r.agenticScope(), truncate(out));
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError e) {
        // The whole cause chain, not just getMessage(): see Errors.
        String msg = e.error() == null ? "error" : Errors.explain(e.error());
        emit("agent-error", e.agentName(), "error in " + e.agentName() + ": " + msg,
                e.agenticScope(), null);
    }

    /** Manually push an error event (used when a pattern run throws). */
    public void emitError(String agent, String message) {
        emit("agent-error", agent, message, null, null);
    }

    private void emit(String type, String agent, String message, AgenticScope scope, Object data) {
        sink.accept(RunEvent.of(seq.getAndIncrement(), type, agent, message, snapshot(scope), data));
    }

    /**
     * Snapshot scope state into a small, JSON-safe map (values stringified and truncated).
     * Keys prefixed {@code __} are LangChain4j's internal planner bookkeeping
     * ({@code __planner_state_*}) — they are noise in a panel meant to teach shared state.
     */
    static Map<String, Object> snapshot(AgenticScope scope) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (scope == null) {
            return out;
        }
        try {
            for (Map.Entry<String, Object> e : scope.state().entrySet()) {
                if (e.getKey().startsWith("__")) {
                    continue;
                }
                out.put(e.getKey(), truncate(e.getValue()));
            }
        } catch (Exception ignore) {
            // scope may be in an inconsistent state during teardown; ignore.
        }
        return out;
    }

    static Object truncate(Object value) {
        String s = String.valueOf(value);
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }
}
