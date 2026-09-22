package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface EverydayCare {
    @Agent(description = "Answers the ordinary questions about living with a dog")
    @UserMessage("""
            You answer everyday dog questions — food, grooming, kit, routine. Give a short,
            practical answer.
            You are the cheapest person to ask and you only know the ordinary things — food,
            kit, grooming, routine. End with exactly one word on its own line: ANSWERED if you
            fully covered it, or ESCALATE if it is past you.

            Worry: {{Worry}}""")
    String handle(@K(Keys.Worry.class) String worry);
}
