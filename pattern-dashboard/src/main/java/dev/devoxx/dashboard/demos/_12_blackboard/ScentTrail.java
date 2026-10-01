package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ScentTrail {
    @Agent(description = "The Bloodhound: adds where the trail leads, and where it does not")
    @UserMessage("""
            You are the Bloodhound. Add the trail to the board: follow it from where the cake
            was to where it ends, and say where it leads and where it does not. Two or three
            lines, the trail only — other dogs cover the rest.

            The crime: {{Crime}}""")
    String add(@K(Keys.Crime.class) String crime);
}
