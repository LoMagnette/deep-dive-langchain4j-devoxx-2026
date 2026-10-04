package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** Beliefs in, intentions out, as a real interface. */
public interface ZoomsHead {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Keys.Beliefs.class) String beliefs);
}
