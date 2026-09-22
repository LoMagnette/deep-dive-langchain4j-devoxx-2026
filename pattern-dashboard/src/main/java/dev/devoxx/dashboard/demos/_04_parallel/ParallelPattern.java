package dev.devoxx.dashboard.demos._04_parallel;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>parallel</b> demo — two independent checks, and a join that DECIDES.
 */
public final class ParallelPattern {

    private ParallelPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        CurrentRun.set(listener);
        ParallelSystem app;
        try {
            app = AgenticServices.createAgenticSystem(ParallelSystem.class, model);
        } finally {
            CurrentRun.clear();
        }
        return app.plan(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "the stay", "input"),
                        node("meals", "ChowHound", "agent"),
                        node("walks", "LeadDeveloper", "agent"),
                        // The combiner is the whole second half of "fan out, then join": the
                        // note cannot be written until both halves are back, which is the only
                        // reason the two branches have to meet again at all.
                        node("join", "both halves", "join")),
                List.of(edge("in", "meals"), edge("in", "walks"),
                        edge("meals", "join", "meals"), edge("walks", "join", "walks")));
        return new PatternDef("parallel", "Parallel", "workflow",
                "The note needs two halves with nothing to say to each other: what he eats, "
                        + "and when he goes out. Two threads, nothing shared, no locks.",
                "Takes the card demo 1 produced. Both planners come back in the capstone.",
                "Fan out independent work concurrently, then join. The meals do not depend on "
                        + "the walks and the walks do not depend on the meals, but the note needs "
                        + "both before it can be written — which is exactly when "
                        + "fan-out-and-join is the right shape. Both agents come back unchanged "
                        + "in the capstone, with a router and a refinement loop around them.",
                "Only for truly independent sub-tasks, and the joining is on you. Watch the two "
                        + "timings against the run's: that gap is the entire reason to reach for "
                        + "this instead of a sequence.",
                topo,
                // Literally what the first demo prints. The chain runs through the DEFAULT
                // INPUTS, not at run time, so skipping a demo on stage never strands the next
                // one and a deep link from a slide still works on its own.
                """
                        Dog: Zao, Bouvier des Flandres
                        Meals: two scoops morning and evening, food in the tub by the back door
                        Walks: not given
                        Watch out for: no dried liver treats; never off the lead in the park
                        Vet: 061 22 33 44""",
                ParallelPattern::run);
    }
}
