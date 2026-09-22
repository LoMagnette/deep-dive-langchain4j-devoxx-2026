package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ChowHound {
    @Agent(name = "ChowHound", description = "Plans the dog's meals for the days the owners are away",
           typedOutputKey = Keys.Meals.class)
    @UserMessage("""
            Plan the dog's meals for the days the owners are away: times, amounts, and
            anything he must not be given. Be brief.

            The stay: {{Stay}}""")
    String plan(@K(Keys.Stay.class) String stay);
}
