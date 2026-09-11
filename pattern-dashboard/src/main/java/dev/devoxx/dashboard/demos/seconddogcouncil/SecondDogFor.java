package dev.devoxx.dashboard.demos.seconddogcouncil;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SecondDogFor {
    @Agent(description = "Argues for the motion in front of the council")
    @UserMessage("""
            Argue for this motion, and answer the strongest objection to it honestly. Two or
            three sentences.

            Motion: {{motion}}""")
    String argue(@V("motion") String motion);
}
