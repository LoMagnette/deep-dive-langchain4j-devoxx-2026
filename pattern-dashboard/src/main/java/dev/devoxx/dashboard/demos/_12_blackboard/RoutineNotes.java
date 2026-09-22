package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface RoutineNotes {
    @Agent(name = "RoutineNotes", description = "Adds what changed in the household recently",
           typedOutputKey = Keys.Routine.class)
    @UserMessage("""
            Add the "what changed" angle to the board: new working hours, someone moved out, a
            different feeding time, a moved bed — and what you would try next. Two or three
            lines, changes only.

            Problem: {{Problem}}""")
    String add(@K(Keys.Problem.class) String problem);
}
