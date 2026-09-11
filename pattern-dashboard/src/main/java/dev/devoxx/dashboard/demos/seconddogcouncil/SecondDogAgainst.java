package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SecondDogAgainst {
    @Agent(description = "Argues against the motion in front of the council")
    @UserMessage("""
            Argue against this motion, and answer the strongest point in its favour honestly.
            Two or three sentences.

            Motion: {{motion}}""")
    String argue(@V("motion") String motion);
}
