package dev.devoxx.dashboard.demos._10_goap;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DoorbellDecoy {
    @Agent(description = "The Beagle: gets the human out of the kitchen, and needs nothing to start")
    @UserMessage("""
            You are the Beagle, and you have the decoy step of the heist: get the human out of
            the kitchen. You need nothing from anyone to do it — you go to the front door and
            howl as if someone is there. Say what you do and how the pack knows the kitchen is
            empty. Two or three lines, in the first person.

            Goal: {{Goal}}""")
    String step(@K(Keys.Goal.class) String goal);
}
