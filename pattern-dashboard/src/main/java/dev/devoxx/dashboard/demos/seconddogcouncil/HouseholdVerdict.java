package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.devoxx.dashboard.demos.debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface HouseholdVerdict {
    @Agent(description = "Rules on the council's motion, and on what condition")
    @UserMessage("""
            You chair the household council. Rule on the motion, name the one fact that
            decided it, and set one condition.

            Motion: {{Motion}}""")
    String rule(@K(Motion.class) String motion);
}
