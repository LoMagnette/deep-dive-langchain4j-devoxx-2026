package dev.devoxx.dashboard.demos._14_debate;

import dev.devoxx.dashboard.demos._14_debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The two-advocate debate, as a real interface, not {@code UntypedAgent}. No {@code .name(...)}
 * on the builder — like every other {@code plannerBuilder()} demo, the wrapper reports itself
 * under this method's name, and the tests filter that noise out under "invoke".
 */
public interface Debate {
    @Agent
    String invoke(@K(Motion.class) String motion);
}
