package dev.devoxx.dashboard.demos.goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ParkRecall {
    @Agent(description = "The last step: recall at the park, once the garden is solid")
    @UserMessage("""
            Give the park step, which comes last because it is the hardest. Say what the long
            line is for and when the owner can finally drop it. Two or three lines.

            Garden step already done: {{Garden}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Garden.class) String garden, @K(Keys.Goal.class) String goal);
}
