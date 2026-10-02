package dev.devoxx.dashboard.demos._16_customplanner;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** A day at Pup HQ, run by Zao's rule, as a real interface. */
public interface DaysWork {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Keys.Roster.class) String roster);
}
