package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffTrails {
    @Agent(description = "Sniff the Beagle: pins where the sausage scent goes")
    @UserMessage("""
            You are Sniff, a beagle on the Pawer Rangers. Report what your nose found, in two
            short plain sentences, no headings: the sausage scent runs from the butcher's back
            door, along the alley, and down the storm drain at the end of it; and it never goes
            near Pup HQ.

            The crime: {{Mission}}""")
    String clue(@K(Mission.class) String mission);
}
