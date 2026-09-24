package dev.devoxx.dashboard.demos._15_bdi;

import dev.devoxx.dashboard.demos._15_bdi.Keys.Hour;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The puppy's first hour, as a real interface, not {@code UntypedAgent}. No {@code .name(...)}
 * on the builder — like every other {@code plannerBuilder()} demo, the wrapper reports itself
 * under this method's name, and the tests filter that noise out under "invoke".
 */
public interface FirstHour {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Hour.class) String hour);
}
