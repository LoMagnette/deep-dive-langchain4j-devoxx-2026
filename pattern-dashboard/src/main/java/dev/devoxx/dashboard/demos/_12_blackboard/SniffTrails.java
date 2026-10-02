package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffTrails {
    @Agent(description = "Sniff the Beagle: pins the scent trail from the scene of the crime")
    @UserMessage("""
            Pin your clue on the Pup Board: follow the sausage scent from the scene of the crime
            and say where it goes, and where it does NOT go. Two plain lines, the scent only —
            the other Rangers cover the rest.

            The crime: {{Mission}}""")
    String clue(@K(Mission.class) String mission);
}
