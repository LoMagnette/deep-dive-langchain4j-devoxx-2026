package dev.devoxx.dashboard.demos._10_goap;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Height;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The planner-ordered mission, as a real interface. No {@code .name(...)} on the builder — like
 * every {@code plannerBuilder()} demo, the wrapper reports itself under this method's name.
 */
public interface GoapMission {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Height.class) double height);
}
