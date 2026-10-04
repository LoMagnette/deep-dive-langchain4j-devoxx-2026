package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/** The three-way fan-out, as a real interface. */
public interface Inspections {
    @Agent
    String inspect(@V("Mission") String mission);
}
