package dev.devoxx.dashboard.demos.blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HomeNotes {
    @Agent(description = "Adds what the dog can see and hear from inside the house")
    @UserMessage("""
            Add the angle of what he can see and hear from indoors: the window onto the
            street, the post, next door's cat, deliveries — and what you would try next. Two
            or three lines, the house only.

            Problem: {{problem}}""")
    String add(@V("problem") String problem);
}
