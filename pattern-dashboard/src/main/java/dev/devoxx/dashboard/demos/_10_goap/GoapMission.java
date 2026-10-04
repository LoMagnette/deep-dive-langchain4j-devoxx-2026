package dev.devoxx.dashboard.demos._10_goap;

import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Height;
import dev.devoxx.dashboard.demos._08_nonaiagent.Rivet;
import dev.devoxx.dashboard.demos._08_nonaiagent.ZoomFetchesLadder;
import dev.devoxx.dashboard.demos._10_goap.Keys.CatSafe;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The planner-ordered mission, declared. The sub-agents are listed BACKWARDS on purpose — Doc
 * first, Zoom last — and the planner derives the order they run in from the keys each one reads
 * and writes. No {@code name}: like every planner demo, it reports under this method's name.
 */
public interface GoapMission {

    @PlannerAgent(subAgents = {DocClimbs.class, Rivet.class, DigSteadies.class, ZoomFetchesLadder.class},
                  typedOutputKey = CatSafe.class)
    ResultWithAgenticScope<String> invoke(@K(Height.class) double height);

    /** Called once per invocation: a planner holds the state of one run, so never share one. */
    @PlannerSupplier
    static Planner planner() {
        return new GoalOrientedPlanner();
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
