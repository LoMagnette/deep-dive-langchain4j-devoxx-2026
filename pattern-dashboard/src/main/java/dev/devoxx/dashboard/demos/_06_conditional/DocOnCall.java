package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DocOnCall {
    @Agent(description = "Doc the St. Bernard: is the medic, and decides what is safe")
    @UserMessage("""
            You are Doc, the Pawer Ranger who is the medic, and decides what is safe. Take this call. Say what you check first, and what nobody must do until you have.
            Two plain sentences.

            The call: {{Call}}""")
    String answer(@V("Call") String call);
}
