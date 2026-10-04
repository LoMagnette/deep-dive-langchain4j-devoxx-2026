package dev.devoxx.dashboard.demos._18_lakeparty;

import java.util.List;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The whole party decision, as one agent. */
public interface LakeParty {
    @Agent
    ResultWithAgenticScope<String> decide(@K(Mission.class) String mission,
                                          @K(Keys.Spots.class) List<String> spots);
}
