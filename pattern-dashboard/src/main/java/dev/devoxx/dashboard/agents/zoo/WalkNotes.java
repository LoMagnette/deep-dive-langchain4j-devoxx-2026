package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>10 — the barking problem</b>
 *
 * <p>Debugging, which is what a blackboard is for. Three note-takers who each read ONLY the
 * problem, so any of them can go first and the board collects three different KINDS of
 * knowledge. Chain them instead and you have a sequence wearing a blackboard's coat.
 */
public interface WalkNotes {
    @Agent(description = "Adds what the dog's exercise explains about the problem")
    @UserMessage("""
            Add the exercise angle to the board: how much he actually gets, whether it is
            enough for his breed and age, and what you would try next. Two or three lines,
            exercise only — other people cover the rest.

            Problem: {{problem}}""")
    String add(@V("problem") String problem);
}
