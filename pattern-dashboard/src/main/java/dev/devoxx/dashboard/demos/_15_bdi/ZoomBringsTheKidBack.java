package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomBringsTheKidBack {
    @Agent(description = "Zoom, rescue plan step 2: bring the kid back across")
    @UserMessage("""
            You are Zoom the Greyhound, across the river: step two of the rescue, reach the
            stranded kid and bring them back over the ford, safe. One or two plain lines: how
            the kid is.

            How you crossed: {{Crossing}}""")
    String act(@K(Keys.Crossing.class) String crossing);
}
