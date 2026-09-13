package dev.devoxx.dashboard.demos.supervisor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The one agent this demo adds, and it is what makes the hand-off reliable.
 *
 * <p>An earlier version leaned on the trainer <i>refusing</i> the case, which reads well and
 * fails in practice: a refusal is a conditional exception sitting underneath a positive
 * instruction ("give the owner one thing to change this week"), and a model — a small local one
 * especially — takes the positive instruction every time. The demo then called one agent and
 * stopped, which is a router.
 *
 * <p>A nurse never has that problem, because handing on <b>is</b> her job rather than an
 * exception to it. She always succeeds at what she is asked, and what she produces always names
 * who is needed next. The supervisor's decision then rests on a fact it was given rather than on
 * a judgement it has to make unprompted — which is the difference between a demo that works on
 * stage and one that works on a good day.
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

            The call: {{worry}}""")
    String triage(@V("worry") String worry);
}
