package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;

/** The three rounds, as an agent of their own — nested inside the council sequence. */
public interface Rounds {
    @Agent
    String argue();
}
