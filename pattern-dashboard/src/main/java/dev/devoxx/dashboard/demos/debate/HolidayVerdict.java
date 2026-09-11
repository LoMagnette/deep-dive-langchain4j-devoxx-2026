package dev.devoxx.dashboard.demos.debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HolidayVerdict {
    @Agent(description = "Settles whether the dog comes on the holiday, and on what condition")
    @UserMessage("""
            Having heard both sides, rule whether the dog comes or stays. Name the one fact
            that decided it, and set one condition on the decision.

            Motion: {{motion}}""")
    String rule(@V("motion") String motion);
}
