package dev.devoxx.dashboard.demos._17_sitternote;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/**
 * The capstone spine — route, plan, merge, refine — as a real interface, not {@code UntypedAgent}.
 * The same text goes in under both keys: the router and specialists ask "what is the worry",
 * the two planners ask "what is the stay".
 */
public interface SitterNotePipeline {
    @Agent
    String write(@K(Worry.class) String worry, @K(Stay.class) String stay);
}
