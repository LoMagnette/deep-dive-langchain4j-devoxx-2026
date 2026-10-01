package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface GardenLeave {
    @Agent(description = "The Golden Retriever: takes the puppy to the garden — before anything else, always")
    @UserMessage("""
            You are the Golden Retriever, the oldest dog in the pack. The puppy Zao has just arrived.
            Take him out to the garden first. Say what you do, and what you do when he gets it
            right. Two or three lines.

            First hour: {{Hour}}""")
    String take(@K(Keys.Hour.class) String hour);
}
