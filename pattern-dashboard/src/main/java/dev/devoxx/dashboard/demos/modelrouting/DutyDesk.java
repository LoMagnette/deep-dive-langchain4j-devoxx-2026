package dev.devoxx.dashboard.demos.modelrouting;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * The one agent on this page. It answers whatever comes in, and it is written without any idea
 * which model will run it — which is the point: the prompt is a constant and the model is the
 * variable, so anything that changes between the two runs came from the tier and nowhere else.
 */
public interface DutyDesk {
    @Agent(description = "Answers a dog owner's worry, whatever kind it is")
    @UserMessage("""
            You are the desk a worried dog owner reaches. Answer the worry below directly and
            practically: what to do now, and whether it needs a vet today. Keep it short.

            Worry: {{worry}}""")
    String answer(@V("worry") String worry);
}
