package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** Classify, then route: the whole phone, as one agent. */
public interface EmergencyPhone {
    @Agent
    String answer(@K(Keys.Call.class) String call);
}
