package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LeaveHimAdvocate {
    @Agent(description = "Argues for leaving the dog at home with a sitter")
    @UserMessage("""
            Argue for leaving the dog at home with a sitter. Make your best case, then answer
            the strongest objection to leaving him honestly rather than dodging it. Two or
            three sentences.

            Motion: {{motion}}""")
    String argue(@V("motion") String motion);
}
