package dev.devoxx.dashboard.demos.voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface AskZaoHimself {
    @Agent(description = "Votes on a second dog from the point of view of the dog they have")
    @UserMessage("""
            Should this household get a second dog? Judge ONLY from the point of view of the
            dog they already have — does he actually like other dogs? Ignore everything else.
            Answer with one word: YES or LATER.

            Household: {{household}}""")
    String vote(@V("household") String household);
}
