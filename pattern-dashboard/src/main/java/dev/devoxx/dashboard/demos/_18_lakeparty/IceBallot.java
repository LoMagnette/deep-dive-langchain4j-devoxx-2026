package dev.devoxx.dashboard.demos._18_lakeparty;

import dev.devoxx.dashboard.demos._13_voting.DocVotes;
import dev.devoxx.dashboard.demos._13_voting.RivetVotes;
import dev.devoxx.dashboard.demos._13_voting.SniffVotes;
import dev.devoxx.dashboard.demos._13_voting.VotingPattern;
import dev.langchain4j.agentic.declarative.PlannerAgent;
import dev.langchain4j.agentic.declarative.PlannerSupplier;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.planner.Planner;

/**
 * Mission 13's vote, nested as one step: the same three voters, the same VETO strategy. Its
 * verdict is pinned this time, so the announcement can read it.
 */
public interface IceBallot {

    @PlannerAgent(subAgents = {SniffVotes.class, DocVotes.class, RivetVotes.class},
                  typedOutputKey = Keys.IceVerdict.class)
    String vote();

    @PlannerSupplier
    static Planner planner() {
        return new VotingPlanner(VotingPattern.VETO);
    }
}
