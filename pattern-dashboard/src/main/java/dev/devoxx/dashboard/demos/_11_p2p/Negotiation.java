package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._11_p2p.Keys.Counter;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Question;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The two-peer negotiation, as a real interface, not {@code UntypedAgent}. No {@code .name(...)}
 * on the builder — like every other {@code plannerBuilder()} demo, the wrapper reports itself
 * under this method's name, and the tests filter that noise out under "invoke".
 */
public interface Negotiation {
    @Agent
    ResultWithAgenticScope<String> invoke(
            @K(Question.class) String question, @K(Counter.class) String counter);
}
