package dev.devoxx.dashboard.agents.composite;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WalkPlanner {
    @Agent(description = "Plans the dog's walks for the days the owners are away")
    @UserMessage("""
            Plan the dog's walks for the days the owners are away: when, how long, on or off
            the lead, and anywhere to avoid. Be brief.

            The stay: {{stay}}""")
    String plan(@V("stay") String stay);
}
