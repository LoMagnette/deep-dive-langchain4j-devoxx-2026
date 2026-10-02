package dev.devoxx.dashboard.demos._08_nonaiagent;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Height;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;

/** Bolt → Zoom, as a real interface, not {@code UntypedAgent}. */
public interface LadderRun {
    @Agent
    String fetch(@K(Height.class) double height);
}
