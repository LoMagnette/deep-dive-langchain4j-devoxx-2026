package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The optional step. It declares {@code meds}, and most stays never write that key — so most
 * runs skip this agent entirely and the note goes out without a medication paragraph.
 */
public interface MedicationNote {
    @Agent(description = "The Border Collie: writes the medication paragraph for the fridge note")
    @UserMessage("""
            You are the Border Collie. Write the medication paragraph for a pack that has never
            given a dog a tablet.
            Say what, how much, when, and what to do with a refused dose. Four lines at most.

            The medication: {{Meds}}""")
    String paragraph(@K(Keys.Meds.class) String meds);
}
