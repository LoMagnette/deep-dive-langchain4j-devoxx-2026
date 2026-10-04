package dev.devoxx.dashboard.demos._13_voting;

import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.langchain4j.agentic.declarative.AgentListenerSupplier;
import dev.langchain4j.agentic.declarative.K;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;

/**
 * The vote, declared: a nose, a medic and a ruler. Rivet is a plain class among two LLM agents,
 * and the list cannot tell. Named "invoke" on the page.
 */
public interface IceVote {

    @PlannerAgent(subAgents = {SniffVotes.class, DocVotes.class, RivetVotes.class},
                  typedOutputKey = Keys.Verdict.class)
    ResultWithAgenticScope<String> invoke(@K(Mission.class) String mission);

    /** Every voter at once, then the strategy — whose answer is the verdict. */
    @PlannerSupplier
    static Planner planner() {
        return new VotingPlanner(VotingPattern.VETO);
    }

    /** The run's listener. A static no-arg method is the only hook, hence {@link CurrentRun}. */
    @AgentListenerSupplier
    static AgentListener listener() {
        return CurrentRun.observers();
    }
}
