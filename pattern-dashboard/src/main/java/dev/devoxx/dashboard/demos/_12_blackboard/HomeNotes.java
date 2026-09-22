package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HomeNotes {
    @Agent(description = "Adds what the dog can see and hear from inside the house")
    @UserMessage("""
            Add the angle of what he can see and hear from indoors: the window onto the
            street, the post, next door's cat, deliveries — and what you would try next. Two
            or three lines, the house only.

            Problem: {{Problem}}""")
    String add(@K(Keys.Problem.class) String problem);
}
