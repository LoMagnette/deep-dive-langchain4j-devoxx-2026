package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SniffChecksForest {
    @Agent(name = "Sniff",
           typedOutputKey = Keys.ForestReport.class,
           description = "Sniff the Beagle: inspects the forest path before the storm")
    @UserMessage("""
            Inspect the forest path before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the forest path — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String forest(@K(Mission.class) String mission);
}
