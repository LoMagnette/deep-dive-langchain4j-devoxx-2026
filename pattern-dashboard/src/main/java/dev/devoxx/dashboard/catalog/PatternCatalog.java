package dev.devoxx.dashboard.catalog;

import java.util.List;
import java.util.Optional;

import dev.devoxx.dashboard.catalog.PatternDef.PatternInfo;
import dev.devoxx.dashboard.demos._01_single.SinglePattern;
import dev.devoxx.dashboard.demos._02_sequential.SequentialPattern;
import dev.devoxx.dashboard.demos._03_loop.LoopPattern;
import dev.devoxx.dashboard.demos._04_parallel.ParallelPattern;
import dev.devoxx.dashboard.demos._05_parallelmapper.ParallelMapperPattern;
import dev.devoxx.dashboard.demos._06_conditional.ConditionalPattern;
import dev.devoxx.dashboard.demos._07_humanapproval.HumanApprovalPattern;
import dev.devoxx.dashboard.demos._08_nonaiagent.NonAiAgentPattern;
import dev.devoxx.dashboard.demos._09_supervisor.SupervisorPattern;
import dev.devoxx.dashboard.demos._10_goap.GoapPattern;
import dev.devoxx.dashboard.demos._11_p2p.P2pPattern;
import dev.devoxx.dashboard.demos._12_blackboard.BlackboardPattern;
import dev.devoxx.dashboard.demos._13_voting.VotingPattern;
import dev.devoxx.dashboard.demos._14_debate.DebatePattern;
import dev.devoxx.dashboard.demos._15_bdi.BdiPattern;
import dev.devoxx.dashboard.demos._16_customplanner.CustomPlannerPattern;
import dev.devoxx.dashboard.demos._17_megamutt.MegaMuttPattern;
import dev.devoxx.dashboard.demos._18_lakeparty.LakePartyPattern;
import dev.devoxx.dashboard.demos._19_modelrouting.ModelRoutingPattern;
import dev.devoxx.dashboard.demos._20_async.AsyncPattern;
import dev.devoxx.dashboard.demos._21_resilience.ResiliencePattern;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The registry, and nothing else: what is in the catalogue and in what order — which is also the
 * order the rail and the gallery show, and the order the talk runs in.
 */
@ApplicationScoped
public class PatternCatalog {

    private final List<PatternDef> patterns = build();

    private static List<PatternDef> build() {
        // In mission-number order, so _NN_ in every package name is its place here. The rail and
        // the gallery group these by the spec's four ACTS (the category), which is how missions
        // 1 and 8 end up side by side in Act 1 without anything being renumbered.
        return List.of(
                SinglePattern.define(),          // 1  · Act 1, Meet the team
                SequentialPattern.define(),      // 2  · Act 2, Workflows — you decide the order
                LoopPattern.define(),            // 3
                ParallelPattern.define(),        // 4
                ParallelMapperPattern.define(),  // 5
                ConditionalPattern.define(),     // 6
                HumanApprovalPattern.define(),   // 7
                NonAiAgentPattern.define(),      // 8  · Act 1 — some pups don't need a brain
                SupervisorPattern.define(),      // 9  · Act 3, Planners — the system decides
                GoapPattern.define(),            // 10
                P2pPattern.define(),             // 11
                BlackboardPattern.define(),      // 12
                VotingPattern.define(),          // 13 · Act 4, Many minds and custom brains
                DebatePattern.define(),          // 14
                BdiPattern.define(),             // 15
                CustomPlannerPattern.define(),   // 16
                // The Mega Mutt — missions combined into a bigger Ranger.
                MegaMuttPattern.define(),        // 17
                LakePartyPattern.define(),       // 18
                // Running it for real — modifiers you can bolt onto any mission above, which is
                // why they sit outside the four acts rather than inside them.
                ModelRoutingPattern.define(),    // 19
                AsyncPattern.define(),           // 20
                ResiliencePattern.define());     // 21
    }

    public List<PatternInfo> infos() {
        return patterns.stream().map(PatternDef::toInfo).toList();
    }

    public Optional<PatternDef> byId(String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
