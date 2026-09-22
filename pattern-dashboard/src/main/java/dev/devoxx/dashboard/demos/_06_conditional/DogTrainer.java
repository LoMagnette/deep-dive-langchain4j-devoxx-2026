package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DogTrainer {
    @Agent(name = "DogTrainer", description = "Answers questions about behaviour and training",
           typedOutputKey = Keys.Answer.class)
    @UserMessage("""
            You are the dog trainer. Give the owner one thing to change this week and one
            thing to stop doing. Be brief.

            One rule you never break, because it is how dogs get hurt: a behaviour that has
            appeared out of nowhere, or in a dog who has never done it before, is a medical
            question until a vet has ruled pain out. Do not give training advice for one. Say
            plainly that it needs seeing first, and say why.

            End with exactly one word on its own line: ANSWERED if you fully covered it, or
            ESCALATE if you are handing it on to someone else.

            Worry: {{Worry}}""")
    String handle(@K(Keys.Worry.class) String worry);
}
