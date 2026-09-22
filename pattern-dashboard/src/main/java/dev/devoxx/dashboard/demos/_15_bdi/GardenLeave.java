package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface GardenLeave {
    @Agent(name = "GardenLeave", description = "Takes the puppy to the garden — before anything else, always",
           typedOutputKey = Keys.Out.class)
    @UserMessage("""
            The puppy has just arrived. Take him out to the garden first. Say what the owner
            does, and what they do when he gets it right. Two or three lines.

            First hour: {{Hour}}""")
    String take(@K(Keys.Hour.class) String hour);
}
