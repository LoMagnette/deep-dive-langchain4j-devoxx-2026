package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The council meeting, as a real interface. No {@code .name(...)} on the builder — like every
 * {@code plannerBuilder()} demo, the wrapper reports itself under this method's name.
 */
public interface Debate {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Keys.Motion.class) String motion);
}
