package dev.devoxx.dashboard.agents.workflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>3 — the refinement loop</b>
 *
 * <p>The critic scores against FOUR RULES THE ROOM AGREES WITH ON SIGHT. That is the whole point:
 * a critic scoring "quality" out of 1.0 gives a number nobody watching can check, so the loop
 * becomes a light show. Here the audience can hold the draft against the same four rules and
 * see which one each pass fixes.
 */
public interface SitterNoteWriter {
    @Agent(description = "Rewrites a note for the dog sitter until it is actually usable")
    @UserMessage("""
            Rewrite this note so someone who has never met the dog could follow it. All four
            rules must hold:
            1. every meal has a time and an amount
            2. it says where the lead and the poo bags are
            3. it gives the vet's telephone number
            4. under 100 words, so it fits on the fridge door

            Note: {{note}}""")
    String rewrite(@V("note") String note);
}
