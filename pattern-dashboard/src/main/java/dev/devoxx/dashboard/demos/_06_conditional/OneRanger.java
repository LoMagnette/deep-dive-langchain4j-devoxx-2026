package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;

/** The conditional step: four Rangers on the bench, one sent. */
public interface OneRanger {
    @Agent
    String answer();
}
