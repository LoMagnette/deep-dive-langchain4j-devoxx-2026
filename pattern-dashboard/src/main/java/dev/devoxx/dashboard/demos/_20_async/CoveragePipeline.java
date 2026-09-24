package dev.devoxx.dashboard.demos._20_async;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The vet-callback-then-note sequence, as a real interface, not {@code UntypedAgent}. */
public interface CoveragePipeline {
    @Agent
    String write(@K(Stay.class) String stay);
}
