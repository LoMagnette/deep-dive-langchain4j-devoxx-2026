package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SpaceAndTime {
    @Agent(name = "SpaceAndTime", description = "Votes on a second dog on space and hours alone",
           typedOutputKey = Keys.SpaceVote.class)
    @UserMessage("""
            Should this household get a second dog? Judge ONLY the space they have and the
            hours the dogs would be alone. Ignore money and ignore the dog they already have.
            Answer with one word: YES or LATER.

            Household: {{Household}}""")
    String vote(@K(Keys.Household.class) String household);
}
