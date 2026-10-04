package dev.devoxx.dashboard.demos._11_p2p;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/** The three-pup search, declared. Named "invoke" on the page — no name on the annotation. */
public interface MazeSearch {

    @PlannerAgent(subAgents = {ZoomInTheMaze.class, DigInTheMaze.class, SniffInTheMaze.class},
                  typedOutputKey = Keys.Scent.class)
    ResultWithAgenticScope<String> invoke(@K(Mission.class) String mission,
                                          @K(Keys.Clearing.class) String clearing,
                                          @K(Keys.Burrows.class) String burrows);

    /** Nobody in charge: whoever's inputs changed goes next, until either pup finds the goat. */
    @PlannerSupplier
    static Planner planner() {
        return new P2PPlanner(20, P2pPattern::goatFound);
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
