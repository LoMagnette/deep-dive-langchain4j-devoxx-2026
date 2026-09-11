package dev.devoxx.dashboard.demos.sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FridgeChecklist {
    @Agent(description = "Turns a sitter card into the timed checklist that goes on the fridge")
    @UserMessage("""
            Turn this sitter card into the checklist that goes on the fridge door: the times
            of day in order, one line each, nothing the sitter has to work out for themselves.

            Sitter card: {{card}}""")
    String checklist(@V("card") String card);
}
