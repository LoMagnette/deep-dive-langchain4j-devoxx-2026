package dev.devoxx.dashboard.demos._18_seconddogcouncil;

import dev.devoxx.dashboard.demos._14_debate.Keys.Motion;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface PuppyFor {
    @Agent(description = "The Corgi: argues for the motion in front of the council")
    @UserMessage("""
            You are the Corgi, and you would like someone shorter than you. Argue for this motion, and answer the strongest objection to it honestly. Two or
            three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Motion.class) String motion);
}
