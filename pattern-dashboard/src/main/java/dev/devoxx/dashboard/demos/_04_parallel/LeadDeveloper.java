package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LeadDeveloper {
    @Agent(description = "Plans the dog's walks for the days the owners are away")
    @UserMessage("""
            Plan the dog's walks for the days the owners are away: when, how long, on or off
            the lead, and anywhere to avoid. Be brief.

            The stay: {{Stay}}""")
    String plan(@V("Stay") String stay);
}
