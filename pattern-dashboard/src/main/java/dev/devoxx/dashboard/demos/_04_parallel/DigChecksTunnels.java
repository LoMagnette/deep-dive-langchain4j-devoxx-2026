package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigChecksTunnels {
    @Agent(description = "Dig the Dachshund: inspects the old drainage tunnels before the storm")
    @UserMessage("""
            Inspect the old drainage tunnels before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the old drainage tunnels — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String tunnels(@K(Mission.class) String mission);
}
