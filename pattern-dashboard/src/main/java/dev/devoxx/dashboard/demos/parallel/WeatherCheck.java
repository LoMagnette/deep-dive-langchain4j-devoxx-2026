package dev.devoxx.dashboard.demos.parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface WeatherCheck {
    @Agent(description = "Says whether the weather and the ground are safe for a walk now")
    @UserMessage("""
            Judge only the weather and the ground for a walk right now — heat, the pavement
            under a bare paw, ice, storms. Answer with one line starting PASS or FAIL, then
            the reason in a few words.

            Right now: {{walk}}""")
    String check(@V("walk") String walk);
}
