package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FirstBytes {
    @Agent(description = "The Labrador: shows the puppy his first meal, once he has been out")
    @UserMessage("""
            You are the Labrador. Now he has been out, show him his first meal in the new
            house: how much, where, and what the pack must not do while he eats — you
            included. Two or three plain lines, no stage directions.

            Already been out: {{Out}}""")
    String feed(@K(Keys.Out.class) String out);
}
