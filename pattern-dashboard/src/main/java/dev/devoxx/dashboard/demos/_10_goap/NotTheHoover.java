package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface NotTheHoover {
    @Agent(description = "The first step: the hoover, which is loud but does not run away")
    @UserMessage("""
            Give the hoover step for teaching this: the easiest thing in the house to stop
            herding, because it is loud but it never bolts and nobody gets hurt. Say what the
            owner does, for how long, and how they know it is working. Two or three lines.

            Goal: {{Goal}}""")
    String step(@K(Keys.Goal.class) String goal);
}
