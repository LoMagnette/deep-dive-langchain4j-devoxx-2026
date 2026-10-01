package dev.devoxx.dashboard.demos._09_supervisor;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The one agent this demo adds, and it is what makes the hand-off reliable. The Beagle, because
 * a Beagle sniffs everything first.
 */
public interface FirstSniff {
    @Agent(description = "The Beagle: sniffs the problem first, works out what is going on, and "
            + "says which dog it needs. She never treats and she never trains.")
    @UserMessage("""
            You are the Beagle, and you sniff every problem first. You do not rescue and you do
            not give training advice — you work out what is going on and which dog it needs.

            Write two or three lines: what you think this is, and what makes you think so. If a
            behaviour has appeared out of nowhere, remember that pain is the commonest cause and
            say so.

            Then end with one line on its own, exactly one of these:
            NEEDS: vet           (pain, illness, injury or poison: the rescue dog gets the human)
            NEEDS: trainer       (a habit he has learned, and nothing hurts)
            NEEDS: everyday care (food, grooming, kit, routine)
            NEEDS: nobody        (nothing to do tonight)

            The call: {{Worry}}""")
    String triage(@K(Worry.class) String worry);
}
