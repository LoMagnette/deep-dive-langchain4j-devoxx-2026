package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigTunnels {
    @Agent(description = "Dig the Dachshund: pins what the paw prints in the drain tunnel say")
    @UserMessage("""
            Pin your clue on the Pup Board: crawl the drain tunnel under the butcher's and say
            what the paw prints there tell you — how big, how many toes, claws or no claws. Two
            plain lines, the tunnel only.

            The crime: {{Mission}}""")
    String clue(@K(Mission.class) String mission);
}
