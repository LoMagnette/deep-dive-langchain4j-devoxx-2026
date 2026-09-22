package dev.devoxx.dashboard.demos._03_loop;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Notes;
import dev.devoxx.dashboard.demos._02_sequential.FridgeMagnet;
import dev.devoxx.dashboard.run.CurrentRun;
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
        // The whole topology now lives on LoopSystem's annotations, including demo 2's agent
        // reused by class. What is left here is the two things the framework cannot know: which
        // model this run is against, and which run's listener is watching.
        //
        // The model is a parameter, which is clean. The listener is not: the declarative hook
        // for it is a static no-arg method, so it has to be reachable ambiently while the
        // system is BUILT. Build is synchronous on this thread, so the bracket is this narrow —
        // and the finally is not optional, because runs come off a pool of four.
        CurrentRun.set(listener);
        LoopSystem app;
        try {
            app = AgenticServices.createAgenticSystem(LoopSystem.class, model);
        } finally {
            CurrentRun.clear();
        }
        // Typed, so no Map of string keys to build and no cast on the way out — the other half
        // of what the declarative form buys.
        return app.refine(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("loop",
                // Demo 2's agent unchanged, with a critic and a loop drawn round it. Both ways
                // out of the critic: the arc back AND the exit, which is what ends a loop.
                List.of(node("in", "notes", "input"),
                        node("writer", "FridgeMagnet", "agent"),
                        node("check", "RuffDraftCritic", "agent").withSub("4 rules, scored"),
                        node("out", "the note", "join").withSub("or after 5 passes")),
                List.of(edge("in", "writer"), edge("writer", "check", "notes"),
                        edge("check", "writer", "score < 0.8"),
                        edge("check", "out", "score ≥ 0.8")));
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "This is the note you actually sent last time. You can see the four things "
                        + "wrong with it from there. So could they.",
                "Demo 2's FridgeMagnet, unchanged. Nothing about the agent changed; a "
                        + "critic and a loop were drawn around it.",
                "Refine until a quality bar is met. The bar is four rules nobody has to be "
                        + "persuaded of — every meal with a time and an amount, where the lead "
                        + "is, the vet's number, short enough for the fridge door — so the score "
                        + "is a fraction of rules satisfied, and you can see which one each pass "
                        + "fixes.",
                "Can spin forever or oscillate — always cap iterations and define a clear exit. A "
                        + "critic scoring 'quality' out of 1.0 gives you a number nobody in the "
                        + "room can check; score against named rules instead.",
                topo,
                // Fails three of the four rules on sight, which is the point: the audience can
                // count the failures before the first agent runs.
                "just feed him twice like normal and take him out when you can, he knows the "
                        + "routine better than we do honestly. ring me if anything's up!! xx",
                LoopPattern::run);
    }
}
