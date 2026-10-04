package dev.devoxx.dashboard.demos._20_async;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._04_parallel.DigChecksTunnels;
import dev.devoxx.dashboard.demos._04_parallel.Keys.SafetyReport;
import dev.devoxx.dashboard.demos._04_parallel.SniffChecksForest;
import dev.devoxx.dashboard.demos._04_parallel.ZaoMerges;
import dev.devoxx.dashboard.demos._04_parallel.ZoomChecksBridge;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * The inspections in a row, declared. Still a sequence: Sniff is sent FIRST. He just does not
 * hold the other two up, because his report is not needed until Zao reads it — and that read is
 * the join. Which step is async is not here: see {@code AsyncPattern.run}.
 */
public interface StormRound {

    @SequenceAgent(name = "Sequential",
                   subAgents = {SniffChecksForest.class, ZoomChecksBridge.class, DigChecksTunnels.class,
                                ZaoMerges.class},
                   typedOutputKey = SafetyReport.class)
    String inspect(@K(Mission.class) String mission);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
