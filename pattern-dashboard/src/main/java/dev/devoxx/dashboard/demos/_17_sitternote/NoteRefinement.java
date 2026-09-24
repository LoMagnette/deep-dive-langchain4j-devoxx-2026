package dev.devoxx.dashboard.demos._17_sitternote;

import dev.langchain4j.agentic.Agent;

/** The refinement loop, as a real interface — reads the merged note the scope now holds. */
public interface NoteRefinement {
    @Agent
    String refine();
}
