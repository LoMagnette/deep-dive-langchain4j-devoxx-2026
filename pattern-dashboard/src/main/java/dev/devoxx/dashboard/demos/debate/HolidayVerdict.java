package dev.devoxx.dashboard.demos.debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HolidayVerdict {
    @Agent(description = "Settles whether the dog comes on the holiday, and on what condition")
    @UserMessage("""
            Having heard both sides, rule whether the dog comes or stays. Name the one fact
            that decided it, and set one condition on the decision.

            Motion: {{Motion}}""")
    String rule(@K(Keys.Motion.class) String motion);
}
