package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface AlibiCheck {
    @Agent(description = "The Border Collie: adds who was where, and who could physically have done it")
    @UserMessage("""
            You are the Border Collie, and you keep count of every dog in the pack. Add the
            alibis to the board: who could physically have done it, and who could not. Two or
            three lines, alibis only — other dogs cover the rest.

            The crime: {{Crime}}""")
    String add(@K(Keys.Crime.class) String crime);
}
