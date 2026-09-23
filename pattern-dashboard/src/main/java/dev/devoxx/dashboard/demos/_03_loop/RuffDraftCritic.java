package dev.devoxx.dashboard.demos._03_loop;

import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * Returns the score as a {@code String}, not a {@code double}, on purpose: a real chat model
 * answers "I'd rate this **0.85** out of 1.0" often enough that a {@code double} return type
 * blows up the whole loop with an OutputParsingException. The caller extracts the number
 * defensively — see {@link dev.devoxx.dashboard.support.Parsing#score}.
 */
public interface RuffDraftCritic {
    @Agent(description = "Checks a sitter note against the four fridge-door rules")
    @UserMessage("""
            Check this note against four rules: every meal has a time and an amount, it says
            where the lead and poo bags are, it gives the vet's telephone number, and it is
            under 100 words. Give the fraction of rules that hold as a number from 0.0 to
            1.0 — the number only, no words, no explanation, no markdown.

            What we know so far: {{Notes}}""")
    String check(@K(Notes.class) String notes);
}
