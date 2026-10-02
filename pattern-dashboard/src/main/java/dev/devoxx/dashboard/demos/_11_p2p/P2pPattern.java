package dev.devoxx.dashboard.demos._11_p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.found;
import static java.util.stream.Collectors.joining;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._11_p2p.Keys.ClearedAreas;
import dev.devoxx.dashboard.demos._11_p2p.Keys.GoatSighting;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 11</b> — two pups, two collars, no leader.
 */
public final class P2pPattern {

    private P2pPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var sniff = AgenticServices.agentBuilder(SniffInTheMaze.class)
                .chatModel(model)
                .name("Sniff")
                .outputKey(GoatSighting.class)
                .build();
        var zoom = AgenticServices.agentBuilder(ZoomInTheMaze.class)
                .chatModel(model)
                .name("Zoom")
                .outputKey(ClearedAreas.class)
                .build();
        MazeSearch app = AgenticServices.plannerBuilder(MazeSearch.class)
                .subAgents(sniff, zoom)
                // The exit predicate is the only thing that ends this: nobody is in charge, so
                // without it the pups talk until the cap. It reads the CONTENT of the pins — a
                // predicate asking only whether a pin exists is true after the first word — and
                // EITHER pup's pin can end it: whoever finds the goat, found it.
                .planner(() -> new P2PPlanner(10, s -> found(s.readState(GoatSighting.class))
                        || found(s.readState(ClearedAreas.class))))
                .outputKey(GoatSighting.class)
                .listener(listener)
                .build();
        // Seeding ClearedAreas is load-bearing: P2PPlanner activates a pup only once every input
        // it declares is present, so with nothing cleared yet neither could take a first turn.
        var r = app.invoke(input, "(nothing yet — we have just walked in)");
        var scope = r.agenticScope();
        String turns = scope.agentInvocations().stream()
                .filter(i -> List.of("Sniff", "Zoom").contains(i.agentName()))
                .map(i -> "- **" + i.agentName() + "** — " + String.valueOf(i.output()).strip())
                .collect(joining("\n"));
        boolean found = found(scope.readState(GoatSighting.class))
                || found(scope.readState(ClearedAreas.class));
        return (found ? "**Goat found.**" : "**Ten rounds, and still no goat.**")
                + "\n\n" + turns;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The mission reaches BOTH pups, because one arrow into the first would make it the pup
        // in charge — the one claim this pattern exists to deny. The exit is a predicate over
        // the board, checked after every turn, not a decision either pup gets to make.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0).withSub("nobody in charge"),
                        node("sniff", "Sniff", "agent", 1).withSub("nose · goatSighting").as("sniff"),
                        node("zoom", "Zoom", "agent", 1).withSub("legs · clearedAreas").as("zoom"),
                        node("out", "exitCondition", "join", 2).withSub("'FOUND:' from either")),
                List.of(edge("in", "sniff"), edge("in", "zoom"),
                        edge("sniff", "zoom", "where to run"),
                        edge("zoom", "sniff", "cleared · ≤10 turns"),
                        edge("sniff", "out"), edge("zoom", "out", "checked every turn")));
        return new PatternDef("p2p", "Peer-to-Peer", "planner",
                "The Mayor's goat is lost in the corn maze. Sniff and Zoom go in. Nobody is in "
                        + "charge — not even Zao, who is outside, eating corn.",
                null,
                "Peers that trigger each other through shared state: Zoom acts when Sniff pins "
                        + "something new, Sniff acts when Zoom does, and the run ends when the "
                        + "board says `FOUND:`. **No coordinator, no order laid down in advance** "
                        + "— point at the diagram: there is no Zao in it. Watch the roll-call: "
                        + "nose, legs, nose, and the goat.",
                "No hierarchy, so it needs a solid exit predicate or it never terminates — and "
                        + "check the opposite failure just as hard: **a predicate that cannot be "
                        + "false is not an exit condition either.** One that asks whether Sniff's "
                        + "pin *exists* is true after his first sentence, and ends the search with "
                        + "no goat. Read the content. And `P2PPlanner` is reactive: two peers "
                        + "writing ONE shared pin re-fire on their own writes, and race.",
                topo,
                "Paws up, Rangers! The Mayor's goat, Gertrude, has wandered into the giant corn maze "
                        + "at Hillside Farm. She was last heard bleating somewhere in the middle.",
                P2pPattern::run);
    }
}
