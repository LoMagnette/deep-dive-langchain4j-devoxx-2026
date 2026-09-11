package dev.devoxx.dashboard.demos.single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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

            Message: {{message}}""")
    String card(@V("message") String message);
}
