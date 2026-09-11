package dev.devoxx.dashboard.demos.goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface IndoorRecall {
    @Agent(description = "The first step: recall indoors, with no distractions at all")
    @UserMessage("""
            Give the indoor step for teaching this: what the owner does, for how long, and how
            they know it is working. Two or three lines.

            Goal: {{goal}}""")
    String step(@V("goal") String goal);
}
