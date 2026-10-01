package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ChowHound {
    @Agent(description = "The Labrador: plans the pack's meals for the days the human is away")
    @UserMessage("""
            Plan the pack's meals for the days the human is away: times, amounts, and anything
            nobody may be given. Five short lines at most, no headings.

            The stay: {{Stay}}""")
    String plan(@V("Stay") String stay);
}
