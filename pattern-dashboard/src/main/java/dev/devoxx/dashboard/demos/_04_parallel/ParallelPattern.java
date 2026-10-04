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
 * Wiring for <b>Mission 4</b> — three inspections at once, then one report.
 */
public final class ParallelPattern {

    private ParallelPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        return CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(StormWarning.class, model).warn(input).result());
    }

    /**
     * How the page draws it, and what the catalogue shows.
     */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "storm warning", "input"),
                        node("bridge", "Zoom", "agent").withSub("the bridge").as("zoom"),
                        node("forest", "Sniff", "agent").withSub("the forest").as("sniff"),
                        node("tunnels", "Dig", "agent").withSub("the tunnels").as("dig"),
                        // The merge is the whole second half of "fan out, then join": nothing
                        // can be said about the town until all three reports are in.
                        node("join", "Zao", "join").withSub("one safety report").as("zao")),
                List.of(edge("in", "bridge"), edge("in", "forest"), edge("in", "tunnels"),
                        edge("bridge", "join", "bridgeReport"), edge("forest", "join", "forestReport"),
                        edge("tunnels", "join", "tunnelReport")));
        return new PatternDef("parallel", "Parallel", "workflow",
                "A storm is coming. The bridge, the forest and the tunnels all need checking "
                        + "before six, and there is only one afternoon.",
                null,
                "Fan out independent work concurrently, then merge. The three inspections do "
                        + "not need each other, so they run at once on their own executor "
                        + "(`.executor(...)` on the parallel builder) — and the safety report "
                        + "needs all three, which is exactly when fan-out-and-join is the right "
                        + "shape. Zao's merge is a second step in a sequence: **the parallel "
                        + "workflow is itself an agent**, plugged into a bigger one.",
                "Only for truly independent sub-tasks, and the merging is on you. Watch the "
                        + "timings badge: the whole mission against the time the Rangers were "
                        + "busy — three checks in the time of one is the entire reason to reach "
                        + "for this instead of a sequence.",
                topo,
                "Paws up, Rangers! Storm warning for Barkville: winds of 90 km/h from six tonight. "
                        + "Check the river bridge, the forest path and the old drainage tunnels "
                        + "before then.",
                ParallelPattern::run)
                .gist("Independent agents run at the same time; results merged.");
    }
}
