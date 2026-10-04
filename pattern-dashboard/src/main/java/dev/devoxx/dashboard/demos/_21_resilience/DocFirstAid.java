package dev.devoxx.dashboard.demos._21_resilience;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.UserMessage;

/** The optional step: it declares {@code Injuries}, and most days nothing writes it. */
public interface DocFirstAid {
    @Agent(name = "Doc",
           typedOutputKey = Keys.FirstAid.class,
           optional = true,
           description = "Doc the St. Bernard: first aid, only when the report says someone is hurt")
    @UserMessage("""
            Give first aid for the injury below. The Rangers have paws, not hands — nothing that
            needs holding a cloth or opening a cupboard. Three plain lines: what to do now, what
            not to do, and when to fetch Officer Jo.

            The report: {{Injuries}}""")
    String treat(@K(Keys.Injuries.class) String injuries);
}
