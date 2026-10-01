package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** The lookup → write → check sequence, as a real interface, not {@code UntypedAgent}. */
public interface DigPipeline {
    @Agent
    String dig(@K(Mission.class) String mission);
}
