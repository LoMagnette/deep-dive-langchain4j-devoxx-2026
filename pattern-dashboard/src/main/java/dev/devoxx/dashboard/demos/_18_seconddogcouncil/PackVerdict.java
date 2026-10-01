package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.devoxx.dashboard.demos._14_debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface PackVerdict {
    @Agent(description = "The Golden Retriever: rules on the council's motion, and on what condition")
    @UserMessage("""
            You chair the pack council. Rule on the motion — carried or not — name the one fact
            that decided it, and set one condition. Plainly, in four sentences at most — no stage
            directions.

            Motion: {{Motion}}""")
    String rule(@K(Motion.class) String motion);
}
