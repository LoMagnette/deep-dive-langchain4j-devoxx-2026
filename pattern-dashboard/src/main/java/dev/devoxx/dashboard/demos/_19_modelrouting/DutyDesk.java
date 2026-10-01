package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The one agent on this page. It answers whatever comes in, and it is written without any idea
 * which model will run it — which is the point: the prompt is a constant and the model is the
 * variable, so anything that changes between the two runs came from the tier and nowhere else.
 */
public interface DutyDesk {
    @Agent(description = "Zao, on duty: answers the pack's worry, whatever kind it is")
    @UserMessage("""
            You are the dog on duty tonight, the one the pack comes to with a worry. Answer the
            worry below directly and practically: what to do now, and whether the human needs
            waking for the vet. Keep it short.

            Worry: {{Worry}}""")
    String answer(@K(Worry.class) String worry);
}
