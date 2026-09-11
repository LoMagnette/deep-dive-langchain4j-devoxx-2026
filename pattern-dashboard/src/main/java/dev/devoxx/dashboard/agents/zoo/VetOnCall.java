package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface VetOnCall {
    @Agent(description = "The vet: the expensive last rung, and the only one who ends the ladder")
    @UserMessage("""
            You are the vet. You are the last rung of the ladder, so answer as best you can
            whatever is asked, and say plainly if the dog needs to be seen in person.
            Answer in two or three sentences, then end with exactly one word on its own:
            ANSWERED.

            Question: {{question}}""")
    String answer(@V("question") String question);
}
