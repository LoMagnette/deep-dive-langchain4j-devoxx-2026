package dev.devoxx.dashboard.demos._14_debate;

import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The council meeting, declared. Every sub-agent but the LAST is a debater; the last one is the
 * judge — so the order of {@code subAgents} is the contract, here exactly as on the builder.
 */
public interface Debate {

    @PlannerAgent(subAgents = {HowlArgues.class, MarmaladeArgues.class, FifiJudges.class},
                  typedOutputKey = Keys.Verdict.class)
    ResultWithAgenticScope<String> invoke(@K(Keys.Motion.class) String motion);

    /**
     * unanimous() ends the debate early only when both say exactly the same thing, which two
     * sides of an argument never do — so it runs all the rounds.
     */
    @PlannerSupplier
    static Planner planner() {
        return new DebatePlanner(DebatePattern.ROUNDS, ConvergenceStrategy.unanimous());
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
