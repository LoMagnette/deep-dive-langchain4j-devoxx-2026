package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.langchain4j.agentic.Agent;

/** The routed branch, as a real interface — reads whichever desk's draft the scope now holds. */
public interface TriageDesk {
    @Agent
    String draft();
}
