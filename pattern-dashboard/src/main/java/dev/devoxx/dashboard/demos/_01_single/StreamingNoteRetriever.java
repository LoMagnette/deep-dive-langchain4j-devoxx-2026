package dev.devoxx.dashboard.demos._01_single;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * {@link NoteRetriever} with one thing changed: it returns a {@link TokenStream} instead of a
 * String. The prompt is copied word for word, deliberately — the toggle on the page is only
 * honest if the two runs differ in nothing but how the answer arrives.
 */
public interface StreamingNoteRetriever {
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
    TokenStream card(@V("Message") String message);
}
