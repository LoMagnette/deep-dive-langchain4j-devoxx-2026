package dev.devoxx.dashboard.demos.voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface MoneyAndVet {
    @Agent(description = "Votes on a second dog on what two dogs cost")
    @UserMessage("""
            Should this household get a second dog? Judge ONLY what a second dog costs —
            food, insurance, vet bills, boarding when they travel. Ignore everything else.
            Answer with one word: YES or LATER.

            Household: {{Household}}""")
    String vote(@K(Keys.Household.class) String household);
}
