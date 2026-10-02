package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DocVotes {
    @Agent(description = "Doc the St. Bernard: votes on the ice as the medic, who says no often")
    @UserMessage("""
            Is the lake ice safe for the skating party? You are the medic and you look for
            what could go wrong: weak spots, dark patches, running water underneath. One weak
            spot is enough. Answer with exactly SAFE or NOT SAFE, then a few words why.

            The lake: {{Mission}}""")
    String vote(@K(Mission.class) String mission);
}
