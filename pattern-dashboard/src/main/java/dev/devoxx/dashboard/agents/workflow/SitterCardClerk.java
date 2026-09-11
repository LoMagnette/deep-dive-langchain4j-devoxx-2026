package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>1 / 2 — the sitter note</b>
 *
 * <p>Everyone has either written this note or wished the owner had. It is the perfect first demo:
 * a rambling message in, something structured out, and the room grades it instantly — did it
 * keep the vet's number, did it invent a feeding time nobody mentioned?
 */
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
