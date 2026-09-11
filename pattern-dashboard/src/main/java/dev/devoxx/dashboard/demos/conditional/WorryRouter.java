package dev.devoxx.dashboard.demos.conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WorryRouter {
    @Agent(description = "Sends the owner's worry to the one who can actually answer it")
    @UserMessage("""
            Classify this worry about a dog into one of: emergency, training, everyday.
            Anything the dog has eaten that could poison him, and anything about breathing,
            bleeding or collapse, is always emergency. Return one word only.

            Worry: {{worry}}""")
    String classify(@V("worry") String worry);
}
