package dev.devoxx.dashboard.demos._04_parallel;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Bait;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Lookout;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

import java.util.List;

import static dev.devoxx.dashboard.catalog.Topology.*;
import static java.util.Objects.requireNonNullElse;

/**
 * Wiring for the <b>parallel</b> demo — two independent checks, and a join that DECIDES.
 */
public final class ParallelPattern {

    private ParallelPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {

        var bait = AgenticServices.agentBuilder(ChowHound.class)
                .chatModel(model)
                .name("ChowHound")
                .outputKey(Bait.class)
                .build();
        var chase = AgenticServices.agentBuilder(LeadDeveloper.class)
                .chatModel(model)
                .name("LeadDeveloper")
                .outputKey(Lookout.class)
                .build();
        FanOut app = AgenticServices.parallelBuilder(FanOut.class)
                .name("Parallel")
                .subAgents(bait, chase)
                .output(s ->
                        "**The bait**\n\n" + requireNonNullElse(s.readState(Bait.class), "")
                        + "\n\n**The chase**\n\n" + requireNonNullElse(s.readState(Lookout.class), ""))
                .listener(listener)
                .build();
        return app.plan(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "target card", "input"),
                        node("meals", "ChowHound", "agent").withSub("Labrador · the bait"),
                        node("walks", "LeadDeveloper", "agent").withSub("Greyhound · the chase"),
                        // The combiner is the whole second half of "fan out, then join": the
                        // plan cannot be written until both halves are back, which is the only
                        // reason the two branches have to meet again at all.
                        node("join", "both halves", "join")),
                List.of(edge("in", "meals"), edge("in", "walks"),
                        edge("meals", "join", "bait"), edge("walks", "join", "chase")));
        return new PatternDef("parallel", "Parallel", "workflow",
                "The operation needs two halves with nothing to say to each other: the bait, "
                        + "and the chase. Two threads, nothing shared, no locks.",
                "Takes the card demo 1 produced. Both planners come back in the capstone.",
                "Fan out independent work concurrently, then join. The bait does not depend on "
                        + "the chase and the chase does not depend on the bait, but the plan needs "
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
                        Target: the grey squirrel with a bit missing off its tail
                        Where: the big oak by the back fence, and the bird feeder
                        Time: not given
                        Route: down the oak, along the top of the fence, to the bird feeder, back up the oak
                        Watch out for: the cat on the shed roof""",
                ParallelPattern::run);
    }
}
