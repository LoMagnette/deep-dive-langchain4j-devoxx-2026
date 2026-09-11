package dev.devoxx.dashboard.agents.pureagent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TrainingPlanner {
    @Agent(description = "Plans what the dog needs to be taught before then")
    @UserMessage("""
            Plan what the dog needs to be taught, and in what order, before this happens. Be
            brief.

            Request: {{request}}""")
    String plan(@V("request") String request);
}
