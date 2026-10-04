package dev.devoxx.dashboard.demos._04_parallel;

import static java.util.Objects.requireNonNullElse;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._04_parallel.Keys.BridgeReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.ForestReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.SafetyReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.TunnelReport;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * Fan-out then merge: the whole mission, as one agent. The fan-out is itself a declared agent
 * ({@link Inspections}), named here by class like any Ranger.
 */
public interface StormWarning {

    @SequenceAgent(name = "Sequential",
                   subAgents = {Inspections.class, ZaoMerges.class})
    ResultWithAgenticScope<String> warn(@K(Mission.class) String mission);

    /**
     * What the system returns, built from the scope by the workflow itself — the declarative
     * form of {@code .output(scope -> ...)}. An {@code AgenticScope} parameter is injected.
     */
    @Output
    static String report(AgenticScope scope) {
        return "**Safety report**\n\n" + scope.readState(SafetyReport.class)
                + "\n\n---\n\n*Bridge (Zoom):* " + requireNonNullElse(scope.readState(BridgeReport.class), "")
                + "\n\n*Forest (Sniff):* " + requireNonNullElse(scope.readState(ForestReport.class), "")
                + "\n\n*Tunnels (Dig):* " + requireNonNullElse(scope.readState(TunnelReport.class), "");
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
