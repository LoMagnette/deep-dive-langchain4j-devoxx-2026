package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface NoteRetriever {
    @Agent(description = "The Golden Retriever: retrieves the facts from the Beagle's breathless report")
    @UserMessage("""
            Turn this sighting report into a target card with exactly these five lines.
            Invent nothing — if the report does not say, write "not given":
            Target:
            Where:
            Time:
            Route:
            Watch out for:

            Message: {{Message}}""")
    String card(@K(Keys.Message.class) String message);
}
