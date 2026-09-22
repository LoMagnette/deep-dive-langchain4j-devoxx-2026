package dev.devoxx.dashboard.demos.blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface WalkNotes {
    @Agent(description = "Adds what the dog's exercise explains about the problem")
    @UserMessage("""
            Add the exercise angle to the board: how much he actually gets, whether it is
            enough for his breed and age, and what you would try next. Two or three lines,
            exercise only — other people cover the rest.

            Problem: {{Problem}}""")
    String add(@K(Keys.Problem.class) String problem);
}
