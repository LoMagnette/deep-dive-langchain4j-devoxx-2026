package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ChairPusher {
    @Agent(description = "The Bulldog: pushes a chair to the counter, which is far too loud to do with the human in the room")
    @UserMessage("""
            You are the Bulldog, and you have the chair step of the heist: push a kitchen
            chair up against the counter. It scrapes on the tiles, so it can only happen once
            the human has left the kitchen. Say how you push it and where it ends up. Two or
            three lines, in the first person.

            The decoy, already done: {{Decoy}}
            Goal: {{Goal}}""")
    String step(@K(Keys.Decoy.class) String decoy, @K(Keys.Goal.class) String goal);
}
