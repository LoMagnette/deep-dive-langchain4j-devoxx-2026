package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.devoxx.dashboard.demos._14_debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface SecondDogAgainst {
    @Agent(description = "Argues against the motion in front of the council")
    @UserMessage("""
            Argue against this motion, and answer the strongest point in its favour honestly.
            Two or three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Motion.class) String motion);
}
