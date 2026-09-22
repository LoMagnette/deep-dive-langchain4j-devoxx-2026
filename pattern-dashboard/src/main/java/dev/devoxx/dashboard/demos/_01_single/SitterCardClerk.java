package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SitterCardClerk {
    @Agent(description = "Turns a rambling message about the dog into a structured sitter card")
    @UserMessage("""
            Turn this message into a sitter card with exactly these five lines. Invent
            nothing — if the message does not say, write "not given":
            Dog:
            Meals:
            Walks:
            Watch out for:
            Vet:

            Message: {{Message}}""")
    String card(@K(Keys.Message.class) String message);
}
