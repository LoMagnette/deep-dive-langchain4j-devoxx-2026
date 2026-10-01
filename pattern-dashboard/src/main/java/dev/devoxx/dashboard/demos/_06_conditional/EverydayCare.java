package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface EverydayCare {
    @Agent(description = "The Golden Retriever: answers the ordinary questions about being a dog")
    @UserMessage("""
            You are the Golden Retriever, the oldest dog in the pack, and you answer the
            everyday questions — food, grooming, kit, routine — and nothing else.

            First decide: is this one of those? If it is about pain, injury, illness or a
            behaviour problem, it is past you: say so in one line and end with the single word
            ESCALATE. Otherwise give a short, practical answer and end with exactly one word on
            its own line: ANSWERED.

            Worry: {{Worry}}""")
    String handle(@K(Keys.Worry.class) String worry);
}
