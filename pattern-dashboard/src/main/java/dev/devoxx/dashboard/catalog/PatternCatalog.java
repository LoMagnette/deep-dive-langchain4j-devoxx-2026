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
import dev.devoxx.dashboard.demos._17_sitternote.SitterNotePattern;
import dev.devoxx.dashboard.demos._18_seconddogcouncil.SecondDogCouncilPattern;
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
        return List.of(
                // Workflows — you decide the path
                SinglePattern.define(),
                SequentialPattern.define(),
                LoopPattern.define(),
                ParallelPattern.define(),
                ParallelMapperPattern.define(),
                ConditionalPattern.define(),
                HumanApprovalPattern.define(),
                // Last of the workflows because "the model decides nothing at all" is the far
                // left of the dial — not in the group below, which is orthogonal to it.
                NonAiAgentPattern.define(),
                // Pure agents — the model decides the path
                SupervisorPattern.define(),
                // The pattern zoo — planners that decide the turns, the last one ours
                GoapPattern.define(),
                P2pPattern.define(),
                BlackboardPattern.define(),
                VotingPattern.define(),
                DebatePattern.define(),
                BdiPattern.define(),
                CustomPlannerPattern.define(),
                // Putting it together
                SitterNotePattern.define(),
                SecondDogCouncilPattern.define(),
                // Running it for real — modifiers on an agent, not positions on the dial. They
                // come last because they are orthogonal to the autonomy question the rail order
                // asks: any of them can be bolted onto any demo above.
                ModelRoutingPattern.define(),
                AsyncPattern.define(),
                ResiliencePattern.define());
    }

    public List<PatternInfo> infos() {
        return patterns.stream().map(PatternDef::toInfo).toList();
    }

    public Optional<PatternDef> byId(String id) {
        return patterns.stream().filter(p -> p.id().equals(id)).findFirst();
    }
}
