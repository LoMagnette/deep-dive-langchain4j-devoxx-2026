package dev.devoxx.dashboard.demos._07_humanapproval;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._02_sequential.Keys.RescueStatus;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * Plan → ask → act, declared. Officer Jo is named by class between the two Digs, exactly like
 * an agent: the sequence cannot tell that her answer came from a browser. Returns the scope too,
 * because the result shows the plan and what Jo said, not only what Dig did.
 */
public interface RoseRescue {

    @SequenceAgent(name = "Sequential",
                   subAgents = {DigPlans.class, OfficerJo.class, DigActs.class},
                   typedOutputKey = RescueStatus.class)
    ResultWithAgenticScope<String> rescue(@K(Mission.class) String mission);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
