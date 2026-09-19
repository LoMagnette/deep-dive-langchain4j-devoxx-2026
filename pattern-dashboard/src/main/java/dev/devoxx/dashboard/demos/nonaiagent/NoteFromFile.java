package dev.devoxx.dashboard.demos.nonaiagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The one step here that genuinely needs a model: turning a row of fields into something a person
 * can follow at 07:00 in someone else's kitchen. It is given the facts rather than asked for
 * them, which is the arrangement the whole demo is arguing for.
 */
public interface NoteFromFile {
    @Agent(description = "Writes the sitter note from the household's record")
    @UserMessage("""
            Write the note for the dog sitter from the record below. Use the record's numbers
            exactly as they are written — copy them, do not reword or round them. Say nothing
            the record does not contain. Under 120 words.

            The record:
            {{facts}}

            The stay: {{stay}}""")
    String write(@V("facts") String facts, @V("stay") String stay);
}
