package dev.devoxx.dashboard.demos._03_loop;

import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.service.V;

/** The loop as a real interface, not {@code UntypedAgent} — one typed argument in, one typed key out. */
public interface RefinementLoop {
    @Agent
    String refine(@V("Notes") String notes);
}
