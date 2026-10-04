package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/** Classify, then route: the whole phone, as one agent. */
public interface EmergencyPhone {
    @Agent
    String answer(@V("Call") String call);
}
