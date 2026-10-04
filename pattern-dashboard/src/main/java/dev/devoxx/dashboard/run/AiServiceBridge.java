package dev.devoxx.dashboard.run;

import dev.devoxx.dashboard.support.Errors;
import dev.langchain4j.guardrail.GuardrailResult;
import dev.langchain4j.observability.api.event.*;
import dev.langchain4j.observability.api.listener.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mission 0's plain AI service is not an agent, so the agentic {@code AgentListener} never hears
 * from it. LangChain4j has a separate observability API for AI services — one listener per event
 * type, registered with {@code AiServices.registerListeners(...)} — and this bridges those events
 * onto the same {@link StreamingListener} every other mission reports to, so the page lights the
 * service's box, its gear and its guardrails exactly as it does for an agent.
 *
 * <p>Guardrail events are reported as type {@code guardrail}, under the guardrail's class name,
 * which is also its box's label on the diagram.
 */
public final class AiServiceBridge {

    private final StreamingListener out;
    private final String name;
    private final AtomicLong startedAt = new AtomicLong();
    private final AtomicInteger reprompts = new AtomicInteger();

    public AiServiceBridge(StreamingListener out, String name) {
        this.out = out;
        this.name = name;
    }

    /** How many times an output guardrail sent the answer back for another go. */
    public int reprompts() {
        return reprompts.get();
    }

    public List<AiServiceListener<?>> listeners() {
        return List.of(
                (AiServiceStartedListener) e -> started(e),
                (ToolExecutedEventListener) e -> tool(e),
                (InputGuardrailExecutedListener) e -> guardrail(e),
                (OutputGuardrailExecutedListener) e -> outputGuardrail(e),
                (AiServiceCompletedListener) e -> completed(e),
                (AiServiceErrorListener) e -> failed(e));
    }

    private void started(AiServiceStartedEvent e) {
        startedAt.set(System.nanoTime());
        out.emitEvent("agent-before", name, "invoking " + name + " (a plain AI service)", null);
    }

    /** The service only reports a tool AFTER it ran, so both halves are emitted together. */
    private void tool(ToolExecutedEvent e) {
        var req = e.request();
        out.emitEvent("tool-call", name, req.name() + "(" + req.arguments() + ")", null);
        out.emitEvent("tool-result", name, req.name() + " → " + e.resultText(), null);
    }

    private void outputGuardrail(OutputGuardrailExecutedEvent e) {
        boolean again = e.result().isReprompt() || e.result().isRetry();
        if (again) {
            reprompts.incrementAndGet();
        }
        // A reprompt is reported as what it is — the answer sent back for another go — rather
        // than as a block, which is what its FATAL result level would otherwise read as.
        guardrail(e, again ? "reprompt: " : null);
    }

    private void guardrail(GuardrailExecutedEvent<?, ?, ?> e) {
        guardrail(e, null);
    }

    private void guardrail(GuardrailExecutedEvent<?, ?, ?> e, String label) {
        GuardrailResult<?> r = e.result();
        String verdict = r.isSuccess() ? "passed"
                : (label != null ? label : r.isFatal() ? "BLOCKED: " : "failed: ") + r.failures().stream()
                        .map(f -> ((GuardrailResult.Failure) f).message())
                        .reduce((a, b) -> a + "; " + b).orElse("");
        Long took = e.duration() == null ? null : e.duration().toMillis();
        out.emitEvent("guardrail", e.guardrailClass().getSimpleName(), verdict, took);
    }

    private void completed(AiServiceCompletedEvent e) {
        long from = startedAt.get();
        Long took = from == 0 ? null : (System.nanoTime() - from) / 1_000_000;
        out.emitEvent("agent-after", name, "completed " + name, took);
    }

    private void failed(AiServiceErrorEvent e) {
        out.emitEvent("agent-error", name, "error in " + name + ": " + Errors.explain(e.error()), null);
    }
}
