package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface FinalBoarding {
    @Agent(description = "The Golden Retriever: settles whether Zao comes on the holiday, and on what condition")
    @UserMessage("""
            Having heard both sides, rule whether the dog comes or stays. Name the one fact
            that decided it, and set one condition on the decision. Plainly, in four sentences
            at most — no stage directions.

            Motion: {{Motion}}""")
    String rule(@K(Keys.Motion.class) String motion);
}
