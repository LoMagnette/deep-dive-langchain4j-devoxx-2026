package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** Fan-out then merge: the whole mission, as one agent — the Mega Mutt in miniature. */
public interface StormWarning {
    @Agent
    ResultWithAgenticScope<String> warn(@K(Mission.class) String mission);
}
