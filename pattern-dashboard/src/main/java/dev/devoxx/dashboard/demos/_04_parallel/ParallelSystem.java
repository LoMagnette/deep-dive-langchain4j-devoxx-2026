package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Meals;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Fan out, then join. {@code @Output} is the join — still plain Java over what the two agents
 * wrote, and still no model in it, but now it takes the two values as arguments instead of
 * reading them off a scope.
 */
public interface ParallelSystem {

    @ParallelAgent(name = "Parallel", subAgents = {ChowHound.class, LeadDeveloper.class})
    String plan(@K(Stay.class) String stay);

    @Output
    static String bothHalves(@K(Meals.class) String meals, @K(Walks.class) String walks) {
        return "**Meals**\n\n" + (meals == null ? "" : meals)
                + "\n\n**Walks**\n\n" + (walks == null ? "" : walks);
    }

    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.listener();
    }
}
