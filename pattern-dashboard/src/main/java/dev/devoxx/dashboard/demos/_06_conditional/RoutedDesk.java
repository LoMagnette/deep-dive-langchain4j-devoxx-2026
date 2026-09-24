package dev.devoxx.dashboard.demos._06_conditional;

import dev.langchain4j.agentic.Agent;

/** The branch, as a real interface — reads the chosen desk's answer straight from the scope. */
public interface RoutedDesk {
    @Agent
    String answer();
}
