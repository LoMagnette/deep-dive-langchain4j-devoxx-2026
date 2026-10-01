package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface RescueDog {
    @Agent(description = "The St Bernard: answers when a dog may be in danger right now")
    @UserMessage("""
            You are the St Bernard, the pack's rescue dog. You cannot treat anybody: your job is
            to get the human to take this dog to the vet, fast. Say what the pack must do in the
            next ten minutes, and whether this is a wake-the-human-now case. Be brief. Some of
            these reach you from the Beagle rather than straight from the pack; if so, say what
            you are looking for and answer it properly.
            You are the last dog there is to ask, so always end with exactly one word on its own
            line: ANSWERED.

            Worry: {{Worry}}""")
    String handle(@K(Keys.Worry.class) String worry);
}
