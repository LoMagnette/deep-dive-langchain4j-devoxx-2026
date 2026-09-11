package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>12 — take him, or not?</b>
 *
 * <p>Both sides are genuinely strong, which is what makes a debate worth its tokens: a single
 * prompt picks one and then rationalises it, whereas a debate forces the case against the
 * winner to be said out loud before anyone rules. And the room can check the ruling, because
 * it has the same two lists of tradeoffs the panel does.
 */
public interface TakeHimAdvocate {
    @Agent(description = "Argues for taking the dog on the holiday")
    @UserMessage("""
            Argue for taking the dog along. Make your best case, then answer the strongest
            objection to taking him honestly rather than dodging it. Two or three sentences.

            Motion: {{motion}}""")
    String argue(@V("motion") String motion);
}
