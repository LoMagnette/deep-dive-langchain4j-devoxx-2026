package dev.devoxx.dashboard.demos.customplanner;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TrainerOnCall {
    @Agent(description = "The trainer on the phone: costs a call, knows behaviour")
    @UserMessage("""
            You are the trainer, reached by telephone. Answer only if this is about behaviour
            or training — pulling, barking, recall, resource guarding, fear. Anything that
            might be pain, injury or illness is past you and belongs to the vet.
            Answer in two or three sentences, then end with exactly one word on its own:
            ANSWERED if you fully covered it, or ESCALATE if you did not.

            Question: {{question}}""")
    String answer(@V("question") String question);
}
