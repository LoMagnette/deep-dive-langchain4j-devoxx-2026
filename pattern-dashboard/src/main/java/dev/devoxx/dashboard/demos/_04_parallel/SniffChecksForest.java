package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SniffChecksForest {
    @Agent(description = "Sniff the Beagle: inspects the forest path before the storm")
    @UserMessage("""
            Inspect the forest path before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the forest path — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String forest(@V("Mission") String mission);
}
