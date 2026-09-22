package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.devoxx.dashboard.demos.debate.Keys.Verdict;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CouncilNote {
    @Agent(description = "Restates the ruling as the household line the assessors will ratify")
    @UserMessage("""
            Restate this ruling as one line describing the household as it would be if the
            ruling is carried out, so the assessors can vote on it.

            Ruling: {{Verdict}}""")
    String note(@K(Verdict.class) String verdict);
}
