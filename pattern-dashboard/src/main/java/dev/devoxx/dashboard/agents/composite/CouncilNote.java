package dev.devoxx.dashboard.agents.composite;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CouncilNote {
    @Agent(description = "Restates the ruling as the household line the assessors will ratify")
    @UserMessage("""
            Restate this ruling as one line describing the household as it would be if the
            ruling is carried out, so the assessors can vote on it.

            Ruling: {{verdict}}""")
    String note(@V("verdict") String verdict);
}
