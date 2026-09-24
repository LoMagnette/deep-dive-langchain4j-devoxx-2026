package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/** The router-then-desk pipeline, as a real interface, not {@code UntypedAgent}. */
public interface TriagePipeline {
    @Agent
    String answer(@V("Worry") String worry);
}
