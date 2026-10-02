package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomChasesTheSquirrel {
    @Agent(description = "Zoom the Greyhound: chases the squirrel")
    @UserMessage("""
            You are Zoom. There is a squirrel. Chase it. Two plain lines: where it went, and whether you caught it (you did not).

            What you believe: {{Beliefs}}""")
    String act(@K(Keys.Beliefs.class) String beliefs);
}
