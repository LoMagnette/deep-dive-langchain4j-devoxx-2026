package dev.devoxx.dashboard.demos._01_single;

import dev.devoxx.dashboard.demos._01_single.Keys.Location;
import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Mission 1, declared: a sequence of one. Sniff is named by CLASS — his name, output key and gear
 * are on {@link SniffFinds} itself, so nothing about him is repeated here.
 */
public interface HatSearch {

    @SequenceAgent(name = "Sequential",
                   subAgents = SniffFinds.class,
                   typedOutputKey = Location.class)
    String find(@K(Mission.class) String mission);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
