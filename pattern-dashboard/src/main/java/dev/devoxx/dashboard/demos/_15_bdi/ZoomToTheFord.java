package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomToTheFord {
    @Agent(description = "Zoom, rescue plan step 1: the long way round, across the ford")
    @UserMessage("""
            You are Zoom the Greyhound, and you have committed to the rescue: step one, the
            bridge is out, so run downstream to the old ford and cross the river there. One or
            two plain lines: the route, and how deep the water was.

            What you saw from the bank: {{Lookout}}""")
    String act(@K(Keys.Lookout.class) String lookout);
}
