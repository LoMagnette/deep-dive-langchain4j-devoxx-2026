package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface NoteRetriever {
    @Agent(description = "The Border Collie: turns the human's rambling message into a structured pack card")
    @UserMessage("""
            You are the Border Collie, the one in the pack who actually reads things. Turn the
            human's message into a pack card with exactly these five lines. Invent nothing — if
            the message does not say, write "not given":
            Pack:
            Meals:
            Walks:
            Watch out for:
            Vet:

            Message: {{Message}}""")
    String card(@K(Keys.Message.class) String message);
}
