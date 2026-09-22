package dev.devoxx.dashboard.demos._06_conditional;

import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.SequenceAgent;
import dev.langchain4j.agentic.observability.AgentListener;

/**
 * Classify, then dispatch — a two-step sequence whose second step is itself a composed agent.
 * That nesting is the thing to notice: {@link ConditionalDesks} is an ordinary sub-agent here,
 * which is exactly what makes the two composites at the end of the catalogue possible.
 */
public interface ConditionalSystem {

    @SequenceAgent(name = "Sequential",
                   subAgents = {WorryRouter.class, ConditionalDesks.class},
                   typedOutputKey = Answer.class)
    String triage(@K(Worry.class) String worry);

    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.listener();
    }
}
