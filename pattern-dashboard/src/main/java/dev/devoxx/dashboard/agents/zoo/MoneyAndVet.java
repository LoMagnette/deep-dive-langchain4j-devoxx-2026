package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface MoneyAndVet {
    @Agent(description = "Votes on a second dog on what two dogs cost")
    @UserMessage("""
            Should this household get a second dog? Judge ONLY what a second dog costs —
            food, insurance, vet bills, boarding when they travel. Ignore everything else.
            Answer with one word: YES or LATER.

            Household: {{household}}""")
    String vote(@V("household") String household);
}
