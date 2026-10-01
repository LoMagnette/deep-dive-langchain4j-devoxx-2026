package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.devoxx.dashboard.demos._14_debate.Keys.Verdict;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface CouncilNote {
    @Agent(description = "The Border Collie: restates the ruling as the line the assessors will ratify")
    @UserMessage("""
            Restate this ruling as one line describing the pack as it would be if the ruling is
            carried out, so the assessors can vote on it. Write that one line and nothing else —
            no options, no headings.

            Ruling: {{Verdict}}""")
    String note(@K(Verdict.class) String verdict);
}
