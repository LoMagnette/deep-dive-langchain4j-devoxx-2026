package dev.devoxx.dashboard.demos._20_async;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Bait;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Lookout;
import dev.devoxx.dashboard.demos._04_parallel.ChowHound;
import dev.devoxx.dashboard.demos._04_parallel.LeadDeveloper;
import dev.devoxx.dashboard.demos._20_async.Keys.FenceReport;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>asynchronous agent</b> demo — a sequence with one step that does not block.
 */
public final class AsyncPattern {

    private AsyncPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The only line that differs from an ordinary sequence. The agent is not written any
        // differently and the builder is not a different builder — async is one call on this one
        // step, and the two planners below it are demo 4's, unchanged.
        var basset = AgenticServices.agentBuilder(FenceCheck.class)
                .chatModel(model)
                .name("FenceCheck")
                .outputKey(FenceReport.class)
                .async(true)
                .build();
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
        CoveragePipeline app = AgenticServices.sequenceBuilder(CoveragePipeline.class)
                .name("Sequential")
                // Declaration order is still a sequence: the Basset is sent FIRST. He just does
                // not hold the other two up, because his answer is not needed until the plan.
                .subAgents(basset, bait, chase)
                .output(AsyncPattern::plan)
                .listener(listener)
                .build();
        return app.write(input);
    }

    /**
     * The join, and it does not look like one — which is the lesson.
     */
    private static String plan(AgenticScope scope) {
        return "**The bait**\n\n" + requireNonNullElse(scope.readState(Bait.class), "")
                + "\n\n**The chase**\n\n" + requireNonNullElse(scope.readState(Lookout.class), "")
                + "\n\n**The fence** *(the run waited here, and only here)*\n\n"
                + requireNonNullElse(scope.readState(FenceReport.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Stages, not chain: the async step's whole point is that it spans the ones after it, and
        // an edge that skips columns arcs over the top rather than hiding behind the boxes
        // between its ends. That long arc IS the agent's lifetime.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the mission", "input", 0),
                        node("fence", "FenceCheck", "agent", 1).withSub("Basset · async · slow"),
                        node("meals", "ChowHound", "agent", 2).withSub("Labrador · the bait"),
                        node("walks", "LeadDeveloper", "agent", 3).withSub("Greyhound · the chase"),
                        node("join", "the plan", "join", 4).withSub("reads FenceReport")),
                List.of(edge("in", "fence"),
                        edge("fence", "meals", "does not wait"),
                        edge("meals", "walks"),
                        edge("walks", "join"),
                        edge("fence", "join", "the read that joins")));
        return new PatternDef("async", "Asynchronous Agents", "production",
                "Before anyone moves, the Basset checks the fence. It is a long fence and he is "
                        + "a Basset. Nobody blocks the main thread waiting for him.",
                "Demo 4's ChowHound and LeadDeveloper, unchanged — only the Basset is new.",
                "One step in an ordinary sequence marked `async(true)`. The agent is unchanged, "
                        + "the builder is unchanged, and the declaration order is unchanged — the "
                        + "slow step is still asked first. What changes is that it writes an "
                        + "`AsyncResponse` into the scope instead of a value, so **the join is "
                        + "the line that reads the key**, not a step you declare. Watch the badge: "
                        + "the whole run is shorter than the agents were busy, in a sequence.",
                "The waiting moves, it does not disappear — and it moves somewhere less obvious. "
                        + "A failure inside an async agent surfaces at the **read**, which is "
                        + "usually a long way from the step that caused it, and the Scope tab "
                        + "shows `<pending>` until then. Only mark a step async when nothing "
                        + "between it and its reader needs its answer.",
                topo,
                "Operation Squirrel at dawn: the squirrel comes down the big oak by the back fence "
                        + "and runs along the top of it to the bird feeder. Plan the bait and the "
                        + "chase now — but nobody goes out until the Basset has been round the whole "
                        + "fence, and it is a long fence, and he is a Basset.",
                AsyncPattern::run);
    }
}
