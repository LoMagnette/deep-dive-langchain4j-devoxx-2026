package dev.devoxx.dashboard.demos._10_goap;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Ladder;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

public interface DigSteadies {
    @Agent(description = "Dig the Dachshund: digs the ladder's feet in and holds it steady")
    @UserMessage("""
            The ladder is at the foot of the water tower. Dig its feet into the ground and hold
            it steady so it cannot slip. One plain sentence: is it secure?

            The ladder: {{Ladder}}""")
    String steady(@K(Ladder.class) String ladder);
}
