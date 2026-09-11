package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface EverydayCare {
    @Agent(description = "Answers the ordinary questions about living with a dog")
    @UserMessage("""
            You answer everyday dog questions — food, grooming, kit, routine. Give a short,
            practical answer.

            Worry: {{worry}}""")
    String handle(@V("worry") String worry);
}
