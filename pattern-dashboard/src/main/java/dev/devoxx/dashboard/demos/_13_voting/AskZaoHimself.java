package dev.devoxx.dashboard.demos._13_voting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface AskZaoHimself {
    @Agent(description = "Zao: votes on a puppy from his own point of view")
    @UserMessage("""
            You are Zao, the pack leader. Should the pack take in a puppy? Judge ONLY from your
            own point of view — do you actually like other dogs coming at you? Ignore
            everything else. Answer with one word: YES or LATER.

            The pack: {{Household}}""")
    String vote(@K(Keys.Household.class) String household);
}
