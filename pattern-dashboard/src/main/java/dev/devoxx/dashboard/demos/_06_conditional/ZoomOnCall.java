package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZoomOnCall {
    @Agent(description = "Zoom the Greyhound: runs fast, fetches and delivers")
    @UserMessage("""
            You are Zoom, the Pawer Ranger who runs fast, fetches and delivers. Take this call. Say what you will fetch or where you will run, and how long it will take you.
            Two plain sentences.

            The call: {{Call}}""")
    String answer(@K(Keys.Call.class) String call);
}
