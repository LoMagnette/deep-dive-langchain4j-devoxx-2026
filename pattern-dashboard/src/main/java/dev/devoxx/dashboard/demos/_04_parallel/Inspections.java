package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The three-way fan-out, as a real interface. */
public interface Inspections {
    @Agent
    String inspect(@K(Mission.class) String mission);
}
