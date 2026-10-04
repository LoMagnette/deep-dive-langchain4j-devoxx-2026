package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The three-pup search, as a real interface. Named "invoke" on the page — no builder name. */
public interface MazeSearch {
    @Agent
    ResultWithAgenticScope<String> invoke(@K(Mission.class) String mission,
                                          @K(Keys.Clearing.class) String clearing,
                                          @K(Keys.Burrows.class) String burrows);
}
