package dev.devoxx.dashboard.demos._03_loop;

import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
 * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
 * blows up the whole loop with an OutputParsingException. The caller extracts the number
 * defensively — see {@link dev.devoxx.dashboard.support.Parsing#score}.
 */
public interface RuffDraftCritic {
    @Agent(description = "The Poodle: checks the battle plan against the four rules")
    @UserMessage("""
            You are the Poodle, and nothing is ever quite good enough. Check this battle plan
            against four rules: every dog in the pack (Zao, the Beagle, the Labrador, the
            Dachshund, the Greyhound, the Corgi) has a position, nobody goes over the fence, the
            cat is not a target, and it is under 100 words. Give the fraction of rules that hold as a number from 0.0 to
            1.0 — the number only, no words, no explanation, no markdown.

            What we know so far: {{Notes}}""")
    String check(@V("Notes") String notes);
}
