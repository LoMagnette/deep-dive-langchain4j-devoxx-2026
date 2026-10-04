package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/** Fan-out then merge: the whole mission, as one agent — the Mega Mutt in miniature. */
public interface StormWarning {
    @Agent
    ResultWithAgenticScope<String> warn(@V("Mission") String mission);
}
