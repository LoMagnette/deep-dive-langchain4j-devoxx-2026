package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>14 — the escalation ladder (custom planner)</b>
 *
 * <p>Declared cheapest-first, because that order IS the policy in EscalationPlanner. Each one
 * ends with ANSWERED or ESCALATE, which is the only thing the planner reads: the model makes a
 * local judgement about its own competence, and the Java decides what that costs. Everyone
 * already knows this ladder — you look it up, then you ring the trainer, then you ring the vet
 * — and everyone knows you do not start at the vet to ask about kibble.
 */
public interface PuppyBook {
    @Agent(description = "The book on the shelf: free, instant, and only good for the basics")
    @UserMessage("""
            You are the puppy book on the shelf. Answer only if this is ordinary, settled
            information — food, kit, grooming, routine, house-training. Anything about how the
            dog behaves, or anything that might be a health problem, is past you.
            Answer in one or two sentences, then end with exactly one word on its own:
            ANSWERED if you fully covered it, or ESCALATE if you did not.

            Question: {{question}}""")
    String answer(@V("question") String question);
}
