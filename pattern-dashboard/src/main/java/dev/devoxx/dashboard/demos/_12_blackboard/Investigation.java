package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The board, as a real interface. Named "invoke" on the page — no builder name. */
public interface Investigation {
    @Agent
    String invoke(@K(Mission.class) String mission);
}
