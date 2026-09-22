package dev.devoxx.dashboard.demos.resilience;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The optional step. It declares {@code meds}, and most stays never write that key — so most
 * runs skip this agent entirely and the note goes out without a medication paragraph.
 *
 * <p>Nothing here says "optional": that is set on the builder, because whether an argument is
 * reliably present is a fact about the system this agent is wired into, not about the agent.
 */
public interface MedicationNote {
    @Agent(description = "Writes the medication paragraph for the fridge note")
    @UserMessage("""
            Write the medication paragraph for a dog sitter who has never given a dog a tablet.
            Say what, how much, when, and what to do with a refused dose. Four lines at most.

            The medication: {{Meds}}""")
    String paragraph(@K(Keys.Meds.class) String meds);
}
