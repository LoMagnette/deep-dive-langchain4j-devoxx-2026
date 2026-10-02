package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** Three votes at once, then the count — as a real interface. */
public interface IceVote {
    @Agent
    String vote(@K(Mission.class) String mission);
}
