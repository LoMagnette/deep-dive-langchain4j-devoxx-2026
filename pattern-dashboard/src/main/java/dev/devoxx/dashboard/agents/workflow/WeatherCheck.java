package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>4 — "walk him now?"</b>
 *
 * <p>Two checks that plainly do not need each other, and both of which must pass before you put
 * the lead on. That is fan-out and join — and the join is a DECISION (either check can veto),
 * not a string concatenation.
 */
public interface WeatherCheck {
    @Agent(description = "Says whether the weather and the ground are safe for a walk now")
    @UserMessage("""
            Judge only the weather and the ground for a walk right now — heat, the pavement
            under a bare paw, ice, storms. Answer with one line starting PASS or FAIL, then
            the reason in a few words.

            Right now: {{walk}}""")
    String check(@V("walk") String walk);
}
