package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LeadDeveloper {
    @Agent(description = "The Greyhound: plans the pack's walks for the days the human is away")
    @UserMessage("""
            Plan the pack's walks for the days the human is away — in the garden, because nobody
            is holding a lead: when, how long, and anywhere to avoid. Five short lines at most,
            no headings.

            The stay: {{Stay}}""")
    String plan(@V("Stay") String stay);
}
