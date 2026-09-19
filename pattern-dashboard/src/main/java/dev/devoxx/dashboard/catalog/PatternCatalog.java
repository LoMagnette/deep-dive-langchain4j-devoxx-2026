package dev.devoxx.dashboard.catalog;

import java.util.List;
import java.util.Optional;

import dev.devoxx.dashboard.demos.single.SinglePattern;
import dev.devoxx.dashboard.demos.sequential.SequentialPattern;
import dev.devoxx.dashboard.demos.loop.LoopPattern;
import dev.devoxx.dashboard.demos.parallel.ParallelPattern;
import dev.devoxx.dashboard.demos.parallelmapper.ParallelMapperPattern;
import dev.devoxx.dashboard.demos.conditional.ConditionalPattern;
import dev.devoxx.dashboard.demos.supervisor.SupervisorPattern;
import dev.devoxx.dashboard.demos.goap.GoapPattern;
import dev.devoxx.dashboard.demos.humanapproval.HumanApprovalPattern;
import dev.devoxx.dashboard.demos.p2p.P2pPattern;
import dev.devoxx.dashboard.demos.blackboard.BlackboardPattern;
import dev.devoxx.dashboard.demos.voting.VotingPattern;
import dev.devoxx.dashboard.demos.debate.DebatePattern;
import dev.devoxx.dashboard.demos.bdi.BdiPattern;
import dev.devoxx.dashboard.demos.customplanner.CustomPlannerPattern;
import dev.devoxx.dashboard.demos.sitternote.SitterNotePattern;
import dev.devoxx.dashboard.demos.seconddogcouncil.SecondDogCouncilPattern;
import dev.devoxx.dashboard.demos.modelrouting.ModelRoutingPattern;
import dev.devoxx.dashboard.demos.async.AsyncPattern;
import dev.devoxx.dashboard.demos.resilience.ResiliencePattern;
import dev.devoxx.dashboard.catalog.PatternDef.PatternInfo;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The registry, and nothing else: what is in the catalogue and in what order — which is also the
 * order the rail and the gallery show, and the order the talk runs in.
 *
 * <p>One entry per demo, each defined in its own package under {@code demos/}. The package is
 * named after the pattern id, so the deep link a slide points at ({@code #/loop}) names the
 * package to open on stage ({@code demos.loop}).
 *
 * <p>To add a demo: make a package for it, put its agents and its {@code XxxPattern} in there,
 * and add one line here.
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
