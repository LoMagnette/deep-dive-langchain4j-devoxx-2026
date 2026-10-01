package dev.devoxx.dashboard.demos._12_blackboard;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CrimeScene {
    @Agent(description = "The German Shepherd: adds what the evidence left at the scene says")
    @UserMessage("""
            You are the German Shepherd, a retired police dog. Add the scene to the board: what
            the evidence left behind says, and what it does not say. Two or three lines, the
            scene only — other dogs cover the rest.

            The crime: {{Crime}}""")
    String add(@K(Keys.Crime.class) String crime);
}
