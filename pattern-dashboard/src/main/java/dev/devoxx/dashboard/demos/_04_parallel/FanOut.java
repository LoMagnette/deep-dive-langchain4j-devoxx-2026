package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.V;

/** The fan-out as a real interface, not {@code UntypedAgent}. */
public interface FanOut {
    @Agent
    String plan(@V("Stay") String stay);
}
