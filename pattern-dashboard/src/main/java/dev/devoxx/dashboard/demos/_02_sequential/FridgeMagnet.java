package dev.devoxx.dashboard.demos._02_sequential;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * <b>Sequential</b>
 */
public interface FridgeMagnet {
    @Agent(description = "Writes the checklist that goes on the fridge door")
    @UserMessage("""
            Turn this into the checklist that goes on the fridge door: the times of day in
            order, one line each, nothing the sitter has to work out for themselves. All four
            rules must hold:
            1. every meal has a time and an amount
            2. it says where the lead and the poo bags are
            3. it gives the vet's telephone number
            4. under 100 words, so it fits on the door

            What we know so far: {{Notes}}""")
    String checklist(@V("Notes") String notes);
}
