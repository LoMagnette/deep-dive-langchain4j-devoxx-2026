package dev.devoxx.dashboard.demos._01_single;

import dev.devoxx.dashboard.demos._01_single.Keys.Location;
import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.service.TokenStream;

/**
 * The same mission, streamed. Returning {@link TokenStream} here is what lets the stream reach
 * the caller: a planner-based system only hands it out when its own return type is a
 * {@code TokenStream} (or it is untyped) and the streaming agent is the LAST step.
 */
public interface StreamingHatSearch {

    @SequenceAgent(name = "Sequential",
                   subAgents = StreamingSniffFinds.class,
                   typedOutputKey = Location.class)
    TokenStream find(@K(Mission.class) String mission);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
