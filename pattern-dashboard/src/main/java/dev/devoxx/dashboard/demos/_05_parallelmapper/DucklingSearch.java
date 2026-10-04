package dev.devoxx.dashboard.demos._05_parallelmapper;

import java.util.List;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/** The mapper, as a real interface: a list of ducklings in, a list of results out. */
public interface DucklingSearch {
    @Agent
    List<String> search(@V("Ducklings") List<String> ducklings);
}
