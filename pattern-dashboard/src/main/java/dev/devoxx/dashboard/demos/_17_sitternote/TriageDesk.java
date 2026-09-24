package dev.devoxx.dashboard.demos._17_sitternote;

import dev.langchain4j.agentic.Agent;

/** The routed branch, as a real interface — reads whichever desk's answer the scope now holds. */
public interface TriageDesk {
    @Agent
    String answer();
}
