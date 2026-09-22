package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface NotTheChildren {
    @Agent(description = "The second step: the children, who scatter and scream, once the hoover is solid")
    @UserMessage("""
            Give the children step, which comes after the hoover step and must build on it.
            Harder, because children scatter and scream, and to a cattle dog that is a herd
            going the wrong way. Say what changes and what to do when he ignores you. Two or
            three lines.

            Hoover step already done: {{Hoover}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Hoover.class) String hoover, @K(Keys.Goal.class) String goal);
}
