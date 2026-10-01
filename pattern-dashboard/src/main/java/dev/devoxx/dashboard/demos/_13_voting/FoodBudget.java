package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FoodBudget {
    @Agent(description = "The Labrador: votes on a puppy on whether there is enough food")
    @UserMessage("""
            You are the Labrador. Should the pack take in a puppy? Judge ONLY the food — what a
            puppy eats, and whether there is enough to go round. Ignore everything else.
            Answer with one word: YES or LATER.

            The pack: {{Household}}""")
    String vote(@K(Keys.Household.class) String household);
}
