package dev.devoxx.dashboard.demos._17_megamutt;

import dev.langchain4j.agentic.Agent;

/** Mission 3's loop, nested: an agent inside the sequence, like any other step. */
public interface GazetteLoop {
    @Agent
    String polish();
}
