package dev.devoxx.dashboard.demos._02_sequential;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._01_single.SniffFinds;
import dev.devoxx.dashboard.demos._02_sequential.Keys.Article;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Mission 2, declared: four Rangers in a row. Mission 1's Sniff is reused by naming his class —
 * there is no second {@code agentBuilder} chain repeating his model, name, key and gear.
 */
public interface KittenRescue {

    @SequenceAgent(name = "Sequential",
                   subAgents = {SniffFinds.class, ZoomRescues.class, DocChecks.class, HowlWritesStory.class},
                   typedOutputKey = Article.class)
    String rescue(@K(Mission.class) String mission);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
