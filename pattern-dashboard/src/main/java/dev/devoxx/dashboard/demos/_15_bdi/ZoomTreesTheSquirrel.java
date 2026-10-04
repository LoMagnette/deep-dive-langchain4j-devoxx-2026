package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomTreesTheSquirrel {
    @Agent(description = "Zoom, squirrel plan step 2: chase it up a tree")
    @UserMessage("""
            You are Zoom the Greyhound, back on the squirrel: step two, chase it up a tree.
            One or two plain lines: which tree, and whether you caught it (you did not).

            What you saw from the bank: {{Lookout}}""")
    String act(@K(Keys.Lookout.class) String lookout);
}
