package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * Plan → ask → act, as a real interface. Returns the scope too, because the result shows the plan
 * and what Jo said, not only what Dig did: the gap between those is the whole pattern.
 */
public interface RoseRescue {
    @Agent
    ResultWithAgenticScope<String> rescue(@K(Mission.class) String mission);
}
