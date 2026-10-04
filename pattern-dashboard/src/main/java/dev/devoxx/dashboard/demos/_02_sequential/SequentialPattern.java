package dev.devoxx.dashboard.demos._02_sequential;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._01_single.Keys.Mission;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 2</b> — four Rangers, one after another.
 */
public final class SequentialPattern {

    private SequentialPattern() {
    }

    /** Shared with Missions 17 and 21, which put the same kitten up the same tree. */
    public static final String KITTEN =
            "Paws up, Rangers! A kitten is stuck in the oak tree on Main Street and has been "
                    + "meowing since breakfast. The Mayor would like it down before the parade.";

    static String run(ChatModel model, String input, StreamingListener listener) {
        return CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(KittenRescue.class, model).rescue(input));
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Every edge carries the pin, because the pin IS the seam: Doc never sees the location
        // and Howl never sees the rescue, and the labels are how the room sees that they don't.
        Topology.Graph topo = graph("chain",
                List.of(node("in", "mission", "input"),
                        node("sniff", "Sniff", "agent").withSub("finds the kitten").as("sniff"),
                        node("zoom", "Zoom", "agent").withSub("brings the ladder").as("zoom"),
                        node("doc", "Doc", "agent").withSub("checks the kitten").as("doc"),
                        node("howl", "Howl", "agent").withSub("writes the Gazette").as("howl")),
                List.of(edge("in", "sniff"), edge("sniff", "zoom", "location"),
                        edge("zoom", "doc", "rescueStatus"), edge("doc", "howl", "healthReport")));
        return new PatternDef("sequential", "Sequential", "workflow",
                "Paws up! A kitten up the oak on Main Street. Four Rangers, one after another, "
                        + "each reading only the last pin.",
                "Mission 1's Sniff, gear and all — this adds three Rangers after him.",
                "Deterministic pipeline: each agent's output key is the next one's input, and "
                        + "nothing else crosses. Sniff pins `location`, Zoom reads only that and "
                        + "pins `rescueStatus`, Doc reads only that, Howl reads only Doc's report "
                        + "— so the Gazette story is exactly as good as the hand-offs before it. "
                        + "Watch the Scope tab fill in, one pin per Ranger.",
                "Rigid order, and a bad hand-off midway derails everything after it: if Doc "
                        + "forgets to say where the kitten was, Howl cannot know. That is why "
                        + "Doc's prompt tells him to repeat it — the seam is a key, and the key "
                        + "is all the next agent has.",
                topo, KITTEN, SequentialPattern::run)
                .gist("Agents in a fixed order, each reading the last one's output.");
    }
}
