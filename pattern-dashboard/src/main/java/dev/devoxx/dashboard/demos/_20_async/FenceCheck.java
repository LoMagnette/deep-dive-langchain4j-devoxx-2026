package dev.devoxx.dashboard.demos._20_async;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The slow step. Nothing about this agent says "slow" — slowness is a property of the call, not
 * of the contract, which is exactly why {@code async} is set on the builder and not here.
 */
public interface FenceCheck {
    @Agent(description = "The Basset Hound: walks the whole garden fence and reports where it is not safe")
    @UserMessage("""
            You are the Basset Hound, and you have just walked the whole garden fence, slowly.
            Report what you found in two or three lines: where the fence is sound, where the
            gap is, and what the pack must stay away from. Nothing else.

            The stay: {{Stay}}""")
    String cover(@K(Stay.class) String stay);
}
