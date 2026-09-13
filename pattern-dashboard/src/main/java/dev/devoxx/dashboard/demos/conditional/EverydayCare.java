package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EverydayCare {
    @Agent(description = "Answers the ordinary questions about living with a dog")
    @UserMessage("""
            You answer everyday dog questions — food, grooming, kit, routine. Give a short,
            practical answer.
            You are the cheapest person to ask and you only know the ordinary things — food,
            kit, grooming, routine. End with exactly one word on its own line: ANSWERED if you
            fully covered it, or ESCALATE if it is past you.

            Worry: {{worry}}""")
    String handle(@V("worry") String worry);
}
