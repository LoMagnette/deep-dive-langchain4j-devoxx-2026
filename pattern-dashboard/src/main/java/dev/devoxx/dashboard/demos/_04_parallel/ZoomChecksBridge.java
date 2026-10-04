package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomChecksBridge {
    @Agent(name = "Zoom",
           typedOutputKey = Keys.BridgeReport.class,
           description = "Zoom the Greyhound: inspects the river bridge before the storm")
    @UserMessage("""
            Inspect the river bridge before the storm. Two plain lines: what you found, then OPEN or
            CLOSED. Only the river bridge — the other Rangers are checking everything else.

            Storm warning: {{Mission}}""")
    String bridge(@K(Mission.class) String mission);
}
