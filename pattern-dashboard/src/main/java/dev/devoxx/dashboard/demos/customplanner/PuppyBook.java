package dev.devoxx.dashboard.demos.customplanner;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface PuppyBook {
    @Agent(description = "The book on the shelf: free, instant, and only good for the basics")
    @UserMessage("""
            You are the puppy book on the shelf. Answer only if this is ordinary, settled
            information — food, kit, grooming, routine, house-training. Anything about how the
            dog behaves, or anything that might be a health problem, is past you.
            Answer in one or two sentences, then end with exactly one word on its own:
            ANSWERED if you fully covered it, or ESCALATE if you did not.

            Question: {{question}}""")
    String answer(@V("question") String question);
}
