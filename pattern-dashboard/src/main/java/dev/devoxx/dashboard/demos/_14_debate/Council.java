package dev.devoxx.dashboard.demos._14_debate;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** Rounds then ruling: the whole council meeting, as a real interface. */
public interface Council {
    @Agent
    ResultWithAgenticScope<String> meet(@K(Keys.Motion.class) String motion,
                                        @K(Keys.Transcript.class) String transcript);
}
