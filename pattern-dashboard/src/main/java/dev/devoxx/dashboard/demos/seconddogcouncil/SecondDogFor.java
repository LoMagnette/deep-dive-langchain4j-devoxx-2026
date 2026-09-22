package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.devoxx.dashboard.demos.debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SecondDogFor {
    @Agent(description = "Argues for the motion in front of the council")
    @UserMessage("""
            Argue for this motion, and answer the strongest objection to it honestly. Two or
            three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Motion.class) String motion);
}
