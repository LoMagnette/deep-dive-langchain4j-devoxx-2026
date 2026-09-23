package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface LeadDeveloper {
    @Agent(description = "Plans the dog's walks for the days the owners are away")
    @UserMessage("""
            Plan the dog's walks for the days the owners are away: when, how long, on or off
            the lead, and anywhere to avoid. Be brief.

            The stay: {{Stay}}""")
    String plan(@K(Keys.Stay.class) String stay);
}
