package dev.devoxx.dashboard.demos._06_conditional;

import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/** Classify, then route: the whole phone, as one agent. */
public interface EmergencyPhone {

    @SequenceAgent(name = "Sequential",
                   subAgents = {ZaoClassifies.class, OneRanger.class},
                   typedOutputKey = Keys.Response.class)
    String answer(@K(Keys.Call.class) String call);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
