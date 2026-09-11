package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>5 — the picnic blanket</b>
 *
 * <p>The best kind of scatter/gather demo: the room already knows every answer. Grapes and
 * chocolate are dangerous, cheddar and bread are not. The width of the fan-out is data, and
 * the cost caveat needs no explaining either — a shopping list is fifty calls.
 */
public interface FoodSafetyCheck {
    @Agent(description = "Says whether one thing the dog ate is dangerous, and what to do")
    @UserMessage("""
            The dog ate this off the picnic blanket. In one line: is it dangerous for a dog,
            and what should the owner do — nothing, watch him, or ring the vet now?

            He ate: {{food}}""")
    String check(@V("food") String food);
}
