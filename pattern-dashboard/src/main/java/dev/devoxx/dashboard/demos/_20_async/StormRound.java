package dev.devoxx.dashboard.demos._20_async;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The inspections in a row, one of them async, as a real interface. */
public interface StormRound {
    @Agent
    String inspect(@K(Mission.class) String mission);
}
