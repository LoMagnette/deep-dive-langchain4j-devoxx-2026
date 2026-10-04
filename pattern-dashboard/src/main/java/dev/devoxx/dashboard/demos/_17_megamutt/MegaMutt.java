package dev.devoxx.dashboard.demos._17_megamutt;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.demos._01_single.SniffFinds;
import dev.devoxx.dashboard.demos._02_sequential.DocChecks;
import dev.devoxx.dashboard.demos._03_loop.Keys.Draft;
import dev.devoxx.dashboard.demos._03_loop.Keys.Feedback;
import dev.devoxx.dashboard.demos._03_loop.Keys.Rules;
import dev.devoxx.dashboard.demos._08_nonaiagent.Rivet;
import dev.devoxx.dashboard.demos._08_nonaiagent.ZoomFetchesLadder;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The whole Mega Mutt, as one agent: which is the point of the Mega Mutt. Four missions' agents,
 * two pieces of plain-Java glue and a nested loop — seven classes in one list, and the list cannot
 * tell which of them is a model, which is Java and which is a whole workflow.
 */
public interface MegaMutt {

    @SequenceAgent(name = "Sequential",
                   subAgents = {SniffFinds.class,          // Mission 1, gear and all
                                TapeMeasure.class,         // glue: a sentence in, a number out
                                Rivet.class,               // Mission 8, between Sniff and Zoom
                                ZoomFetchesLadder.class,   // Mission 8 — its key rewired in run()
                                DocChecks.class,           // Mission 2, unchanged
                                GazetteBrief.class,        // glue: two pins in, the loop's brief out
                                GazetteLoop.class},        // Mission 3's loop, nested as one step
                   typedOutputKey = Draft.class)
    ResultWithAgenticScope<String> rescue(@K(Mission.class) String mission,
                                          @K(Rules.class) String rules,
                                          @K(Feedback.class) String feedback);

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
