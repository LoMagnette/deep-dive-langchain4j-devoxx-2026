package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The router → duty-desk sequence, as a real interface, not {@code UntypedAgent}. */
public interface DutyDeskPipeline {
    @Agent
    String answer(@K(Worry.class) String worry);
}
