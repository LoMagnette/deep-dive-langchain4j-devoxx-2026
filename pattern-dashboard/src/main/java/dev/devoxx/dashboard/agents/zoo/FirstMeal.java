package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface FirstMeal {
    @Agent(description = "Gives the puppy his first meal, once he has been out")
    @UserMessage("""
            Now he has been out, give him his first meal in the new house: how much, where,
            and what the owner should not do while he eats. Two or three lines.

            Already been out: {{out}}""")
    String feed(@V("out") String out);
}
