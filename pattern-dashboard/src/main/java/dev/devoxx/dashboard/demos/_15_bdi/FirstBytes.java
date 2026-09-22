package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FirstBytes {
    @Agent(name = "FirstBytes", description = "Gives the puppy his first meal, once he has been out",
           typedOutputKey = Keys.Fed.class)
    @UserMessage("""
            Now he has been out, give him his first meal in the new house: how much, where,
            and what the owner should not do while he eats. Two or three lines.

            Already been out: {{Out}}""")
    String feed(@K(Keys.Out.class) String out);
}
