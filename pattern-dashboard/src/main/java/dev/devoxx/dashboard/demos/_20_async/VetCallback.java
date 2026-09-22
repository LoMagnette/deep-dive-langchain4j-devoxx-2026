package dev.devoxx.dashboard.demos._20_async;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The slow step. Nothing about this agent says "slow" — slowness is a property of the call, not
 * of the contract, which is exactly why {@code async} is set on the builder and not here.
 */
public interface VetCallback {
    @Agent(name = "VetCallback", description = "Gets the out-of-hours cover details from the vet's practice",
           typedOutputKey = Keys.VetLine.class)
    @UserMessage("""
            You are the vet practice's out-of-hours desk, answering a slow callback.
            Give the cover arrangements for the dates below in two or three lines: who is on
            call, the number to ring, and how far away they are. Nothing else.

            The stay: {{Stay}}""")
    String cover(@K(Stay.class) String stay);
}
