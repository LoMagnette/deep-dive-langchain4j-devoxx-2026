package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>8 — recall, in three steps</b>
 *
 * <p>A precondition chain nobody has to be told about: you cannot practise recall at the park
 * before it works in the garden, and it will not work in the garden before it works indoors.
 * Three agents whose I/O keys only fit together one way — which is why the planner has
 * something to discover. They are deliberately registered in the WRONG order in the wiring.
 */
public interface IndoorRecall {
    @Agent(description = "The first step: recall indoors, with no distractions at all")
    @UserMessage("""
            Give the indoor step for teaching this: what the owner does, for how long, and how
            they know it is working. Two or three lines.

            Goal: {{goal}}""")
    String step(@V("goal") String goal);
}
