package dev.devoxx.dashboard.demos.sitternote;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MealPlanner {
    @Agent(description = "Plans the dog's meals for the days the owners are away")
    @UserMessage("""
            Plan the dog's meals for the days the owners are away: times, amounts, and
            anything he must not be given. Be brief.

            The stay: {{stay}}""")
    String plan(@V("stay") String stay);
}
