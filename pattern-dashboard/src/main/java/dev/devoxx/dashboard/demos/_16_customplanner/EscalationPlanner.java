package dev.devoxx.dashboard.demos._16_customplanner;

import java.util.List;
import java.util.Locale;

import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;

/**
 * A planner written by hand, to show that the whole pattern zoo is just implementations of one
 * small interface — and that writing your own is the middle of the autonomy dial rather than the
 * deep end of it.
 */
public final class EscalationPlanner implements Planner {

    /** In cost order, cheapest first — that is what declaring them in this order means here. */
    private List<AgentInstance> tiers = List.of();
    private int nextTier = 0;
    private boolean settled = false;

    @Override
    public void init(InitPlanningContext context) {
        tiers = context.subagents();
    }

    /**
     * Two independent stops, and both are needed: one for "somebody answered", one for "we have
     * run out of people to ask". Leave the second out and a planner that never gets a clear
     * answer walks off the end of the list.
     */
    @Override
    public boolean terminated() {
        return settled || nextTier >= tiers.size();
    }

    @Override
    public Action nextAction(PlanningContext context) {
        var previous = context.previousAgentInvocation();
        // Null on the very first call — firstAction() defaults to nextAction(), so this method
        // runs once before any agent has been invoked.
        if (previous != null && answered(previous.output())) {
            settled = true;
            return done(previous.output());
        }
        if (nextTier >= tiers.size()) {
            // Everyone was out of their depth. Ending with done() rather than done(result) hands
            // the caller whatever the last tier wrote to the scope, which is the honest answer:
            // the best we got, not a pretence that it was enough.
            return done();
        }
        return call(tiers.get(nextTier++));
    }

    /**
     * Read defensively, like everything else a model hands back. Asked to end with one word, a
     * real model writes "…so this is ANSWERED." or restates the instruction and mentions both
     * words — so take the LAST marker that appears, which is the one it concluded with, and treat
     * anything unrecognisable as "not answered". Failing towards escalation is the safe
     * direction: the cost of one more call is a call, and the cost of stopping too early is a
     * limping dog nobody looked at.
     */
    private static boolean answered(Object output) {
        String text = String.valueOf(output == null ? "" : output).toUpperCase(Locale.ROOT);
        return text.lastIndexOf("ANSWERED") > text.lastIndexOf("ESCALATE");
    }
}
