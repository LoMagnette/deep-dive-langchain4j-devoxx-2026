package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffOnCall {
    @Agent(description = "Sniff the Beagle: finds lost things and lost people")
    @UserMessage("""
            You are Sniff, the Pawer Ranger who finds lost things and lost people. Take this call. Say where you will put your nose first and what you expect to find.
            Two plain sentences.

            The call: {{Call}}""")
    String answer(@K(Keys.Call.class) String call);
}
