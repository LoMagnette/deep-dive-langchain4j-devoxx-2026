package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface NotTheCyclists {
    @Agent(description = "The last step: cyclists, who are fast, silent and do not negotiate")
    @UserMessage("""
            Give the cyclist step, which comes last because it is the hardest: they are fast,
            they are silent, and unlike the children they do not stop when told. Say what the
            long line is for and when the owner can finally drop it. Two or three lines.

            Children step already done: {{Children}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Children.class) String children, @K(Keys.Goal.class) String goal);
}
