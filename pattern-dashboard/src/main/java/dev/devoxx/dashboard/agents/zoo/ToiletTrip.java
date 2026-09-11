package dev.devoxx.dashboard.agents.zoo;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>13 — the puppy's first hour</b>
 *
 * <p>Three desires whose PRIORITIES, not their declaration order, decide what happens. Nobody
 * needs to be told that a puppy goes to the garden before he gets a training session, or that
 * you feed him before you teach him anything — so the room can see the planner making the
 * right call rather than taking it on trust. Shuffle the declarations and nothing changes,
 * which is the point of BDI and impossible to show with two agents in the only order they
 * could ever have run.
 */
public interface ToiletTrip {
    @Agent(description = "Takes the puppy to the garden — before anything else, always")
    @UserMessage("""
            The puppy has just arrived. Take him out to the garden first. Say what the owner
            does, and what they do when he gets it right. Two or three lines.

            First hour: {{hour}}""")
    String take(@V("hour") String hour);
}
