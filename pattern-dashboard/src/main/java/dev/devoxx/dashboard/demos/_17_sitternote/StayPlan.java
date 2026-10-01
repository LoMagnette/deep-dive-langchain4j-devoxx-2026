package dev.devoxx.dashboard.demos._17_sitternote;

import dev.langchain4j.agentic.Agent;

/** The bait/chase fan-out, as a real interface — both halves read the mission from the scope. */
public interface StayPlan {
    @Agent
    String plan();
}
