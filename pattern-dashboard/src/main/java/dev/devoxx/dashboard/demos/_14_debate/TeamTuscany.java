package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamTuscany {
    @Agent(description = "Argues for taking the dog on the holiday")
    @UserMessage("""
            Argue for taking the dog along. Make your best case, then answer the strongest
            objection to taking him honestly rather than dodging it. Two or three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Keys.Motion.class) String motion);
}
