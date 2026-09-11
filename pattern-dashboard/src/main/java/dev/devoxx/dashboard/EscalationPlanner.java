package dev.devoxx.dashboard;

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
 *
 * <p><b>The policy: ask the cheapest source first, and stop at the first one that can actually
 * answer.</b> Sub-agents are declared in cost order (the book on the shelf, then the trainer on
 * the phone, then the vet). Each one answers and says whether it was out of its depth; this
 * planner reads that and either stops or moves one rung up the ladder.
 *
 * <p>Why this is not one of the built-in builders, which is the only reason to write a planner:
 * <ul>
 *   <li>a <b>sequence</b> would run all three every time — triple the cost, and you would then
 *       have to work out which answer to keep;</li>
 *   <li>a <b>conditional</b> router picks a tier up front from the question alone, and the whole
 *       point here is that you often cannot tell in advance — you find out from what the cheap
 *       attempt came back with;</li>
 *   <li>a <b>loop</b> re-runs the same agents rather than advancing through different ones;</li>
 *   <li>a <b>supervisor</b> could do it, but then an LLM is deciding your cost policy. Escalation
 *       rules are exactly the kind of thing you want in Java where you can read it, test it, and
 *       be sure it never rings the emergency vet to ask which food to buy.</li>
 * </ul>
 *
 * <p>The whole interface is {@link #nextAction}: return {@code call(...)} to invoke agents,
 * {@code done()} or {@code done(result)} to stop. {@code init} hands you the sub-agents in
 * declaration order, and {@link PlanningContext#previousAgentInvocation()} is what makes a
 * planner like this possible at all — it carries what the last agent actually returned, so the
 * next decision can depend on the answer rather than only on the input.
 *
 * <p>Not implemented here, and worth saying on stage: {@code executionState()} /
 * {@code restoreExecutionState(...)} are how a planner survives being suspended and resumed
 * (human-in-the-loop). This one is a demo, so its state is a cursor and a flag in memory.
 */
final class EscalationPlanner implements Planner {

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
