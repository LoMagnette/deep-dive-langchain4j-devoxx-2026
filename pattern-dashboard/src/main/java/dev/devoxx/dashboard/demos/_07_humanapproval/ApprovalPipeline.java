package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The router → triage → human → final-note pipeline, as a real interface, not
 * {@code UntypedAgent}. Returns the scope alongside the result so the caller can show what
 * was drafted and what the person said, not only the outcome.
 */
public interface ApprovalPipeline {
    @Agent
    ResultWithAgenticScope<String> instruct(@K(Worry.class) String worry);
}
