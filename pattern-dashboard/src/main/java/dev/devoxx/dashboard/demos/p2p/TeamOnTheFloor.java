package dev.devoxx.dashboard.demos.p2p;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface TeamOnTheFloor {
    @Agent(description = "Argues for the dog's own bed, and can settle or counter a proposal")
    @UserMessage("""
            You are the half of the household that wants the dog in his own bed. If the other
            half's proposal is one you could actually live with, write the house rule you both
            keep and end with the word AGREED. If it is not, counter it and say why it will
            not last a week.

            Their proposal: {{Proposal}}""")
    String settle(@K(Keys.Proposal.class) String proposal);
}
