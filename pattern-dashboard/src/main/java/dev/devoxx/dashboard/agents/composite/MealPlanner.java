package dev.devoxx.dashboard.agents.composite;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>15 — the weekend away</b>
 *
 * <p>The capstone's own agents. The artefact is the one a household really does produce: a single
 * note on the fridge door that a sitter can follow without ringing you.
 */
public interface MealPlanner {
    @Agent(description = "Plans the dog's meals for the days the owners are away")
    @UserMessage("""
            Plan the dog's meals for the days the owners are away: times, amounts, and
            anything he must not be given. Be brief.

            The stay: {{stay}}""")
    String plan(@V("stay") String stay);
}
