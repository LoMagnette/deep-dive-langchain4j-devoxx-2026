package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/**
 * The optional step. It declares {@code Injuries}, and most operations never write that key — so
 * most runs skip this agent entirely and the plan goes out without a first-aid paragraph.
 */
public interface FirstAidNote {
    @Agent(description = "The St Bernard: writes the first-aid paragraph, when somebody got hurt")
    @UserMessage("""
            Write the first-aid paragraph for the pack about the injury below: what to do now,
            what not to do, and when to wake the human for the vet. The pack has paws, not
            hands — nothing that needs holding a cloth or opening a cupboard. Four plain lines
            at most.

            The injury: {{Injuries}}""")
    String paragraph(@K(Keys.Injuries.class) String injuries);
}
