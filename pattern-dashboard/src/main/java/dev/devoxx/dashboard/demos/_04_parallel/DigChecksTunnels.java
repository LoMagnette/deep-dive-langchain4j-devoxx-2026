package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DigChecksTunnels {
    @Agent(description = "Dig the Dachshund: inspects the old drainage tunnels before the storm")
    @UserMessage("""
            Inspect the old drainage tunnels before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the old drainage tunnels — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String tunnels(@V("Mission") String mission);
}
