package dev.devoxx.dashboard.demos._10_goap;

import dev.devoxx.dashboard.demos._10_goap.Keys.Goal;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The planner-ordered mission, as a real interface, not {@code UntypedAgent}. No {@code .name(...)}
 * on the builder — like every other {@code plannerBuilder()} demo, the wrapper reports itself
 * under this method's name, and the tests filter that noise out under "invoke".
 */
public interface GoapMission {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Goal.class) String goal);
}
