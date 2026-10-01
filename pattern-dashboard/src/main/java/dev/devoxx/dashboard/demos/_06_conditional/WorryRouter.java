package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface WorryRouter {
    @Agent(description = "The Corgi: herds the worry to the one dog who can actually answer it")
    @UserMessage("""
            Classify this worry about a dog into one of: emergency, training, everyday.
            Anything the dog has eaten that could poison him or block his gut, any sting, bite or
            swelling, a dog stuck or trapped anywhere, and anything about breathing, bleeding, a
            limp or collapse, is always emergency. Return one word only.

            Worry: {{Worry}}""")
    String classify(@K(Keys.Worry.class) String worry);
}
