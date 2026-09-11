package dev.devoxx.dashboard.demos.sitternote;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SitterNoteMerger {
    @Agent(description = "Merges the answer and the two plans into one note for the sitter")
    @UserMessage("""
            Write the note that goes on the fridge for the dog sitter. Put the thing that
            matters most at the top.

            What the expert said: {{answer}}
            Meals: {{meals}}
            Walks: {{walks}}""")
    String write(@V("answer") String answer, @V("meals") String meals,
                 @V("walks") String walks);
}
