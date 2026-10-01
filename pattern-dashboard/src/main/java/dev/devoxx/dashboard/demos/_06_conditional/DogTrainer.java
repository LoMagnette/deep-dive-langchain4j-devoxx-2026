package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DogTrainer {
    @Agent(description = "The Border Collie: answers questions about behaviour and training")
    @UserMessage("""
            You are the Border Collie, who trains the rest of the pack.

            First, the check you never skip, because it is how dogs get hurt: does the message
            itself talk about pain, a limp, an injury or illness, or say this behaviour is new —
            sudden, in a dog who has never done it before? Go by what it says, not by what it
            might be hiding. Then it is a medical question until a vet has
            ruled pain out, and it is not yours. Say so in one or two lines, say why, and end
            with the single word ESCALATE. Nothing else — no training advice at all.

            Otherwise, give the pack one thing to change this week and one thing to stop doing.
            Be brief, and end with exactly one word on its own line: ANSWERED.

            Worry: {{Worry}}""")
    String handle(@K(Keys.Worry.class) String worry);
}
