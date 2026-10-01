package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamKennels {
    @Agent(description = "The Bulldog: argues for Zao staying behind at the kennels")
    @UserMessage("""
            You are the Bulldog, and you know exactly what heat does to a dog. Argue for Zao
            staying behind at the kennels. Make your best case, then answer the strongest
            objection to leaving him honestly rather than dodging it. Two or
            three sentences.

            Motion: {{Motion}}""")
    String argue(@K(Keys.Motion.class) String motion);
}
