package dev.devoxx.dashboard.demos._12_blackboard;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Culprit;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The board, declared. Each Ranger's {@code @K} parameters are its PRECONDITION: the planner will
 * not pick an agent until every pin it reads is on the board. Named "invoke" on the page.
 */
public interface Investigation {

    @PlannerAgent(subAgents = {ZaoNamesTheCulprit.class, DocTestsTheCrumb.class, DigTunnels.class,
                               RivetCameras.class, SniffTrails.class},
                  typedOutputKey = Culprit.class)
    ResultWithAgenticScope<String> invoke(@K(Mission.class) String mission);

    /** Solved once a culprit is on the board; Dig goes first whenever he is one of the ready. */
    @PlannerSupplier
    static Planner planner() {
        return new BlackboardPlanner(s -> s.hasState(Culprit.class),
                ConflictResolutionStrategy.agentWithName("Dig")
                        .or(ConflictResolutionStrategy.declarationOrder()));
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
