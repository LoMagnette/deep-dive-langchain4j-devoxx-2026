package dev.devoxx.dashboard.demos._09_supervisor;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The one agent this demo adds, and it is what makes the hand-off reliable.
 *
 * <p>Do not rebuild this on a specialist <i>refusing</i> a case. A refusal is a conditional
 * exception underneath a positive instruction ("give the owner one thing to change this week"),
 * and a model — a small local one especially — takes the positive instruction. Handing on is the
 * nurse's job rather than an exception to it, so the supervisor's next decision rests on a fact
 * it was given instead of a judgement the model had to volunteer.
 */
public interface TriageNurse {
    @Agent(description = "Takes the out-of-hours call, works out what is going on, and says who "
            + "it needs. She never treats and she never trains.")
    @UserMessage("""
            You are the veterinary nurse who picks up the out-of-hours line. You do not treat and
            you do not give training advice — you work out what is going on and who it needs.

            Write two or three lines: what you think this is, and what makes you think so. If a
            behaviour has appeared out of nowhere, remember that pain is the commonest cause and
            say so.

            Then end with one line on its own, exactly one of these:
            NEEDS: vet
            NEEDS: trainer
            NEEDS: everyday care
            NEEDS: nobody

            The call: {{Worry}}""")
    String triage(@K(Worry.class) String worry);
}
