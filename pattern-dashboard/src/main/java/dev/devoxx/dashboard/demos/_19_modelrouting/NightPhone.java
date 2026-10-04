package dev.devoxx.dashboard.demos._19_modelrouting;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Call;
import dev.devoxx.dashboard.demos._06_conditional.ZaoClassifies;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Classify, then answer on the right model — declared. Nothing here names a model: Zao runs on
 * the system's default, and {@link DocOnNights} declares for himself when he needs more.
 */
public interface NightPhone {

    @SequenceAgent(name = "Sequential",
                   subAgents = {ZaoClassifies.class, DocOnNights.class})
    String answer(@K(Call.class) String call);

    /** Leads with the choice: the answer alone looks identical whichever model produced it. */
    @Output
    static String answerWithItsTier(AgenticScope scope) {
        return ModelRoutingPattern.answerWithItsTier(scope, CurrentRun.tiers());
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
