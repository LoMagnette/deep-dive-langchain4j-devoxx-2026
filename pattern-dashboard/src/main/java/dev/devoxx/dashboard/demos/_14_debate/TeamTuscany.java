package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamTuscany {
    @Agent(description = "The Labrador: argues for Zao going on the holiday")
    @UserMessage("""
            You are the Labrador, and you have never once refused a car journey. Argue for Zao
            going along. Make your best case, then answer the strongest objection to taking him
            honestly rather than dodging it. Two or three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Keys.Motion.class) String motion);
}
