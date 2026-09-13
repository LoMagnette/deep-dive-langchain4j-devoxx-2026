package dev.devoxx.dashboard.demos.sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>Sequential</b>
 *
 * <p>The one agent three demos share, and the reason the chain is worth building. Here it turns
 * the clerk's card into the list on the fridge door. The <b>loop</b> demo puts the very same
 * agent in a loop with a critic and lets it rewrite its own output until the four rules hold —
 * no new agent, just different control around it. The capstone uses it a third time.
 *
 * <p>It reads {@code notes}, not "card", deliberately: whatever we know so far, in whatever shape
 * it is currently in. That is what lets the loop feed the agent its own output.
 */
public interface FridgeChecklist {
    @Agent(description = "Writes the checklist that goes on the fridge door")
    @UserMessage("""
            Turn this into the checklist that goes on the fridge door: the times of day in
            order, one line each, nothing the sitter has to work out for themselves. All four
            rules must hold:
            1. every meal has a time and an amount
            2. it says where the lead and the poo bags are
            3. it gives the vet's telephone number
            4. under 100 words, so it fits on the door

            What we know so far: {{notes}}""")
    String checklist(@V("notes") String notes);
}
