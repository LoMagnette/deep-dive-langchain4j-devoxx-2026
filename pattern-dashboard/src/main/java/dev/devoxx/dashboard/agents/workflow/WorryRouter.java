package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>6 — triage the worry</b>
 *
 * <p>Routing where mis-routing is obviously expensive, which is the only kind worth a router. "He
 * ate a bar of dark chocolate" must not reach the trainer, and the room knows that without
 * being told — so it can judge the classifier itself.
 */
public interface WorryRouter {
    @Agent(description = "Sends the owner's worry to the one who can actually answer it")
    @UserMessage("""
            Classify this worry about a dog into one of: emergency, training, everyday.
            Anything the dog has eaten that could poison him, and anything about breathing,
            bleeding or collapse, is always emergency. Return one word only.

            Worry: {{worry}}""")
    String classify(@V("worry") String worry);
}
