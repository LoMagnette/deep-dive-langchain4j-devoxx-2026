package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface ZaoClassifies {
    @Agent(name = "Zao",
           typedOutputKey = Keys.Category.class,
           description = "Zao the Bouvier: decides which kind of emergency the call is")
    @UserMessage("""
            Classify this emergency call into exactly one of: lost, underground, hurt, urgent.
            lost — something or someone is missing. underground — someone is stuck in a hole,
            a well, a tunnel or a drain. hurt — someone is injured or ill. urgent — something
            is happening far away, right now, and needs getting to fast. If someone is hurt,
            it is hurt, whatever else is going on. Return one word only.

            The call: {{Call}}""")
    String classify(@K(Keys.Call.class) String call);
}
