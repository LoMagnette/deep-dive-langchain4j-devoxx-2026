package dev.devoxx.dashboard.demos._15_bdi;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomRescuesTheKid {
    @Agent(description = "Zoom the Greyhound: rescues the stranded kid, the long way round the broken bridge")
    @UserMessage("""
            You are Zoom, and you have committed to the rescue. The bridge is out, so take the long way round to the kid, and bring them back safe. Two plain lines: the route you ran, and how the kid is.

            What you believe: {{Beliefs}}""")
    String act(@K(Keys.Beliefs.class) String beliefs);
}
