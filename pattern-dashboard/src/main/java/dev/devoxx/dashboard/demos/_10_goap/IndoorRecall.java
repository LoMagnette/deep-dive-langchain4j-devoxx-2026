package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface IndoorRecall {
    @Agent(description = "The first step: recall indoors, with no distractions at all")
    @UserMessage("""
            Give the indoor step for teaching this: what the owner does, for how long, and how
            they know it is working. Two or three lines.

            Goal: {{Goal}}""")
    String step(@K(Keys.Goal.class) String goal);
}
