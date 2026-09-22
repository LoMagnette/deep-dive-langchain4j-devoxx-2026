package dev.devoxx.dashboard.demos._17_sitternote;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Meals;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
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
