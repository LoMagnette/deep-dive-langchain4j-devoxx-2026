package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SofaSpace {
    @Agent(description = "The Greyhound: votes on a puppy on the space there is")
    @UserMessage("""
            You are the Greyhound. Should the pack take in a puppy? Judge ONLY the space there
            is — the sofa, the beds, the floor in front of the radiator. Ignore food and ignore
            how Zao feels about it. Answer with one word: YES or LATER.

            The pack: {{Household}}""")
    String vote(@K(Keys.Household.class) String household);
}
