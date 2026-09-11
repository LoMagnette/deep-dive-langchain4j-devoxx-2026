package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface HouseholdVerdict {
    @Agent(description = "Rules on the council's motion, and on what condition")
    @UserMessage("""
            You chair the household council. Rule on the motion, name the one fact that
            decided it, and set one condition.

            Motion: {{motion}}""")
    String rule(@V("motion") String motion);
}
