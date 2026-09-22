package dev.devoxx.dashboard.demos.goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface GardenRecall {
    @Agent(description = "The second step: recall in the garden, once indoors is solid")
    @UserMessage("""
            Give the garden step, which comes after the indoor step and must build on it. Say
            what changes and what to do if he ignores the call. Two or three lines.

            Indoor step already done: {{Indoor}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Indoor.class) String indoor, @K(Keys.Goal.class) String goal);
}
