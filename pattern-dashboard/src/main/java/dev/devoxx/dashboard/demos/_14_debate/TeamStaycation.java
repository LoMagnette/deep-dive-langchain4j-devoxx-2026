package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamStaycation {
    @Agent(description = "Argues for leaving the dog at home with a sitter")
    @UserMessage("""
            Argue for leaving the dog at home with a sitter. Make your best case, then answer
            the strongest objection to leaving him honestly rather than dodging it. Two or
            three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Keys.Motion.class) String motion);
}
