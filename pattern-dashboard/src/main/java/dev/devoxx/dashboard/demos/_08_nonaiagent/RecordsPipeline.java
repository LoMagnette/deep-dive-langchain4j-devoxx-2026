package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._04_parallel.Keys.Stay;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The lookup → write → check sequence, as a real interface, not {@code UntypedAgent}. */
public interface RecordsPipeline {
    @Agent
    String write(@K(Stay.class) String stay);
}
