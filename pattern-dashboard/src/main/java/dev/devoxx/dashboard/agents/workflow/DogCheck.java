package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DogCheck {
    @Agent(description = "Says whether the dog himself is fit for a walk right now")
    @UserMessage("""
            Judge only the dog himself for a walk right now — has he just eaten, his age, any
            limp, anything he had done at the vet today. Answer with one line starting PASS or
            FAIL, then the reason in a few words.

            Right now: {{walk}}""")
    String check(@V("walk") String walk);
}
