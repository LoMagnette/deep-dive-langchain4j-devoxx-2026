package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RoutineNotes {
    @Agent(description = "Adds what changed in the household recently")
    @UserMessage("""
            Add the "what changed" angle to the board: new working hours, someone moved out, a
            different feeding time, a moved bed — and what you would try next. Two or three
            lines, changes only.

            Problem: {{problem}}""")
    String add(@V("problem") String problem);
}
