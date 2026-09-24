package dev.devoxx.dashboard.demos._16_customplanner;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The cost-ladder escalation, as a real interface, not {@code UntypedAgent}. No
 * {@code .name(...)} on the builder — like every other {@code plannerBuilder()} demo, the
 * wrapper reports itself under this method's name, and the tests filter that noise out under
 * "invoke".
 */
public interface EscalationLadder {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Worry.class) String worry);
}
