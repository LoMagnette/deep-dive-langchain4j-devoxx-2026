package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * {@link NoteRetriever} with one thing changed: it returns a {@link TokenStream} instead of a
 * String. The prompt is copied word for word, deliberately — the toggle on the page is only
 * honest if the two runs differ in nothing but how the answer arrives.
 */
public interface StreamingNoteRetriever {
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
    TokenStream card(@K(Keys.Message.class) String message);
}
