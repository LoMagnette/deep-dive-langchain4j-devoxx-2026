package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomNaps {
    @Agent(description = "Zoom the Greyhound: naps")
    @UserMessage("""
            You are Zoom, and everything that matters is done. Nap. One plain line: where, and for how long.

            What you believe: {{Beliefs}}""")
    String act(@K(Keys.Beliefs.class) String beliefs);
}
