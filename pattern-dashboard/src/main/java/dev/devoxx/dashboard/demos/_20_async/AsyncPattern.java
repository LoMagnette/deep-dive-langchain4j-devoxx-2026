package dev.devoxx.dashboard.demos._20_async;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._04_parallel.DigChecksTunnels;
import dev.devoxx.dashboard.demos._04_parallel.Keys.BridgeReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.ForestReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.SafetyReport;
import dev.devoxx.dashboard.demos._04_parallel.Keys.TunnelReport;
import dev.devoxx.dashboard.demos._04_parallel.SniffChecksForest;
import dev.devoxx.dashboard.demos._04_parallel.ZaoMerges;
import dev.devoxx.dashboard.demos._04_parallel.ZoomChecksBridge;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 20</b> — Mission 4's inspections in a row, with one of them async.
 */
public final class AsyncPattern {

    private AsyncPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        // The only line that differs from an ordinary sequence. The agent is Mission 4's, the
        // builder is not a different builder — async is one call on this one step.
        var sniff = AgenticServices.agentBuilder(SniffChecksForest.class)
                .chatModel(model)
                .name("Sniff")
                .outputKey(ForestReport.class)
                .async(true)
                .build();
        var zoom = AgenticServices.agentBuilder(ZoomChecksBridge.class)
                .chatModel(model).name("Zoom").outputKey(BridgeReport.class).build();
        var dig = AgenticServices.agentBuilder(DigChecksTunnels.class)
                .chatModel(model).name("Dig").outputKey(TunnelReport.class).build();
        var zao = AgenticServices.agentBuilder(ZaoMerges.class)
                .chatModel(model).name("Zao").outputKey(SafetyReport.class).build();

        StormRound app = AgenticServices.sequenceBuilder(StormRound.class)
                .name("Sequential")
                // Still a sequence: Sniff is sent FIRST. He just does not hold the other two up,
                // because his report is not needed until Zao reads it — and that read is the join.
                .subAgents(sniff, zoom, dig, zao)
                .outputKey(SafetyReport.class)
                .listener(listener)
                .build();
        return "**Safety report** *(the run waited for Sniff here, and only here)*\n\n"
                + app.inspect(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Stages, not chain: the async step's whole point is that it spans the ones after it, and
        // an edge that skips columns arcs over the top rather than hiding behind the boxes between
        // its ends. That long arc IS Sniff's lifetime.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "storm warning", "input", 0),
                        node("forest", "Sniff", "agent", 1).withSub("async · nose down").as("sniff"),
                        node("bridge", "Zoom", "agent", 2).withSub("the bridge").as("zoom"),
                        node("tunnels", "Dig", "agent", 3).withSub("the tunnels").as("dig"),
                        node("join", "Zao", "join", 4).withSub("reads ForestReport").as("zao")),
                List.of(edge("in", "forest"),
                        edge("forest", "bridge", "does not wait"),
                        edge("bridge", "tunnels"),
                        edge("tunnels", "join"),
                        edge("forest", "join", "the read that joins")));
        return new PatternDef("async", "Asynchronous Agents", "production",
                "The storm again. Sniff's forest check is slow — nose down, every tree. Nobody "
                        + "blocks the main thread waiting for a Beagle.",
                "Mission 4's four agents, unchanged — as a sequence this time, with one of them async.",
                "One step in an ordinary sequence marked `async(true)`. The agent is unchanged, "
                        + "the builder is unchanged, and Sniff is still sent first. What changes is "
                        + "that he writes an `AsyncResponse` into the scope instead of a value, so "
                        + "**the join is the line that reads the key** — Zao's merge — not a step "
                        + "you declare. Watch the badge: the whole run is shorter than the "
                        + "Rangers were busy, in a sequence.",
                "The waiting moves, it does not disappear — and it moves somewhere less obvious. A "
                        + "failure inside an async agent surfaces at the **read**, a long way from "
                        + "the step that caused it, and the Scope tab shows `<pending>` until then. "
                        + "Only mark a step async when nothing between it and its reader needs it.",
                topo,
                "Paws up, Rangers! Storm warning for Barkville: winds of 90 km/h from six tonight. "
                        + "Check the forest path, the river bridge and the old drainage tunnels.",
                AsyncPattern::run);
    }
}
