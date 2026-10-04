package dev.devoxx.dashboard.demos._04_parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ZoomChecksBridge {
    @Agent(description = "Zoom the Greyhound: inspects the river bridge before the storm")
    @UserMessage("""
            Inspect the river bridge before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the river bridge — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String bridge(@V("Mission") String mission);
}
