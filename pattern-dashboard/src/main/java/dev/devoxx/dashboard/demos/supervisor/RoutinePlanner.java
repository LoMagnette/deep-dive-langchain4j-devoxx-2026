package dev.devoxx.dashboard.demos.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface RoutinePlanner {
    @Agent(description = "Changes the dog's daily routine to fit what is coming")
    @UserMessage("""
            Plan the changes to the dog's daily routine — walks, feeding, where he sleeps,
            where he is when the house is busy. Be brief.

            Request: {{request}}""")
    String plan(@V("request") String request);
}
