package dev.devoxx.dashboard.demos._03_loop;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.score;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._02_sequential.BattlePlanner;
import dev.devoxx.dashboard.demos._03_loop.Keys.Score;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>loop</b> demo — refine until four rules the room agrees with are satisfied.
 */
public final class LoopPattern {

    private LoopPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var writer = AgenticServices.agentBuilder(BattlePlanner.class)
                .chatModel(model)
                .name("BattlePlanner")
                .outputKey(Notes.class)
                .build();
        var check = AgenticServices.agentBuilder(RuffDraftCritic.class)
                .chatModel(model)
                .name("RuffDraftCritic")
                .outputKey(Score.class)
                .build();

        RefinementLoop app = AgenticServices.loopBuilder(RefinementLoop.class)
                .name("Loop")
                .subAgents(writer, check)
                .maxIterations(5)
                .exitCondition(s -> score(s.readState(Score.class)) >= 0.8)
                .testExitAtLoopEnd(true)
                .outputKey(Notes.class)
                .listener(listener)
                .build();

        return app.refine(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("loop",
                // Demo 2's agent unchanged, with a critic and a loop drawn round it. Both ways
                // out of the critic: the arc back AND the exit, which is what ends a loop.
                List.of(node("in", "last time's plan", "input"),
                        node("writer", "BattlePlanner", "agent").withSub("Collie · rewrites"),
                        node("check", "RuffDraftCritic", "agent").withSub("Poodle · 4 rules, scored"),
                        node("out", "the plan", "join").withSub("or after 5 passes")),
                List.of(edge("in", "writer"), edge("writer", "check", "plan"),
                        edge("check", "writer", "score < 0.8"),
                        edge("check", "out", "score ≥ 0.8")));
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "This is Zao's plan from last time. The Labrador went over the fence, and the "
                        + "cat has not spoken to anyone since. The Poodle has notes.",
                "Demo 2's BattlePlanner, unchanged. Nothing about the agent changed; a "
                        + "critic and a loop were drawn around it.",
                "Refine until a quality bar is met. The bar is four rules nobody has to be "
                        + "persuaded of — every dog has a position, nobody goes over the fence, "
                        + "the cat is not a target, short enough to bark — so the score "
                        + "is a fraction of rules satisfied, and you can see which one each pass "
                        + "fixes.",
                "Can spin forever or oscillate — always cap iterations and define a clear exit. A "
                        + "critic scoring 'quality' out of 1.0 gives you a number nobody in the "
                        + "room can check; score against named rules instead.",
                topo,
                // Breaks three of the four rules on sight — over the fence, the cat as a target,
                // and three dogs with no position — and keeps the fourth, so the room can count
                // the failures before the first agent runs. Every rule can be fixed without
                // inventing a fact: a version whose fix needed a missing fact (a vet's number) had
                // a live model make one up, and the critic passed it.
                "LAST TIME'S PLAN: everyone just CHASE IT. Labrador, go OVER the fence after it. "
                        + "Dachshund, get the cat on the way past, it had it coming. Beagle, bark. "
                        + "Everyone else: whatever.",
                LoopPattern::run);
    }
}
