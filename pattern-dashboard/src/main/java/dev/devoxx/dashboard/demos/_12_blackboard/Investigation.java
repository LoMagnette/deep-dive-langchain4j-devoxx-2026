package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._12_blackboard.Keys.Problem;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The blackboard investigation, as a real interface, not {@code UntypedAgent}. No
 * {@code .name(...)} on the builder — like every other {@code plannerBuilder()} demo, the
 * wrapper reports itself under this method's name, and the tests filter that noise out under
 * "invoke".
 */
public interface Investigation {
    @Agent
    String invoke(@K(Problem.class) String problem);
}
