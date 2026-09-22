package dev.devoxx.dashboard.demos.sitternote;

import dev.devoxx.dashboard.demos.conditional.Keys.Answer;
import dev.devoxx.dashboard.demos.parallel.Keys.Meals;
import dev.devoxx.dashboard.demos.parallel.Keys.Walks;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SitterNoteMerger {
    @Agent(description = "Merges the answer and the two plans into one note for the sitter")
    @UserMessage("""
            Write the note that goes on the fridge for the dog sitter. Put the thing that
            matters most at the top.

            What the expert said: {{Answer}}
            Meals: {{Meals}}
            Walks: {{Walks}}""")
    String write(@K(Answer.class) String answer, @K(Meals.class) String meals,
                 @K(Walks.class) String walks);
}
