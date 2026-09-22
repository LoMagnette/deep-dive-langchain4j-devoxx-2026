package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface WorryRouter {
    @Agent(name = "WorryRouter", description = "Sends the owner's worry to the one who can actually answer it",
           typedOutputKey = Keys.Category.class)
    @UserMessage("""
            Classify this worry about a dog into one of: emergency, training, everyday.
            Anything the dog has eaten that could poison him, and anything about breathing,
            bleeding or collapse, is always emergency. Return one word only.

            Worry: {{Worry}}""")
    String classify(@K(Keys.Worry.class) String worry);
}
