package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The vote, as a real interface. No {@code .name(...)} on the builder — like every
 * {@code plannerBuilder()} demo, the wrapper reports itself under this method's name.
 */
public interface IceVote {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Mission.class) String mission);
}
