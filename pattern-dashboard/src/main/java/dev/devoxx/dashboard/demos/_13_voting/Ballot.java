package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._13_voting.Keys.Household;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The three-way vote, as a real interface, not {@code UntypedAgent}. No {@code .name(...)} on
 * the builder — like every other {@code plannerBuilder()} demo, the wrapper reports itself
 * under this method's name, and the tests filter that noise out under "invoke".
 */
public interface Ballot {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Household.class) String household);
}
