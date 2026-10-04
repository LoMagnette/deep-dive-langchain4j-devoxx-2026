package dev.devoxx.dashboard.demos._18_lakeparty;

import dev.langchain4j.agentic.Agent;

/** Mission 13's vote, nested as one step. */
public interface IceBallot {
    @Agent
    String vote();
}
