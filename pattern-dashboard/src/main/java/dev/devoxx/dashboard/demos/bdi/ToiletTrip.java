package dev.devoxx.dashboard.demos.bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ToiletTrip {
    @Agent(description = "Takes the puppy to the garden — before anything else, always")
    @UserMessage("""
            The puppy has just arrived. Take him out to the garden first. Say what the owner
            does, and what they do when he gets it right. Two or three lines.

            First hour: {{hour}}""")
    String take(@V("hour") String hour);
}
