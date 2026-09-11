package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DogTrainer {
    @Agent(description = "Answers questions about behaviour and training")
    @UserMessage("""
            You are the dog trainer. Give the owner one thing to change this week and one
            thing to stop doing. Be brief.

            Worry: {{worry}}""")
    String handle(@V("worry") String worry);
}
