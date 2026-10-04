package dev.devoxx.dashboard.demos._11_p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.found;
import static java.util.stream.Collectors.joining;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Burrows;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Clearing;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Scent;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 11</b> — three pups, three collars, no leader.
 */
public final class P2pPattern {

    private P2pPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var sniff = AgenticServices.agentBuilder(SniffInTheMaze.class)
                .chatModel(model).name("Sniff").outputKey(Scent.class).build();
        var zoom = AgenticServices.agentBuilder(ZoomInTheMaze.class)
                .chatModel(model).name("Zoom").outputKey(Clearing.class).build();
        var dig = AgenticServices.agentBuilder(DigInTheMaze.class)
                .chatModel(model).name("Dig").outputKey(Burrows.class).build();

        MazeSearch app = AgenticServices.plannerBuilder(MazeSearch.class)
                // No order here means anything: P2PPlanner runs every pup whose inputs are all
                // on the board, and re-runs a pup whenever one of them CHANGES. Sniff's scent
                // wakes Zoom and Dig together; each of their reports wakes Sniff again.
                .subAgents(zoom, dig, sniff)
                // The exit predicate is the only thing that ends this — nobody is in charge — and
                // it reads the CONTENT of the pins, from any pup: whoever finds the goat, found it.
                .planner(() -> new P2PPlanner(20, P2pPattern::goatFound))
                .outputKey(Scent.class)
                .listener(listener)
                .build();
        // Seeding Zoom's and Dig's pins is load-bearing: a pup only activates once every pin it
        // reads exists, so without them Sniff could never take the first turn — and with Sniff
        // silent, nobody else ever wakes up. The run would end "stable" with nobody having moved.
        var r = app.invoke(input, "(nothing yet — we have just walked in)",
                "(nothing yet — we have just walked in)");
        return roll(r.agenticScope());
    }

    static boolean goatFound(AgenticScope s) {
        return found(s.readState(Scent.class)) || found(s.readState(Clearing.class))
                || found(s.readState(Burrows.class));
    }

    // ---- how the result is presented ----

    /** Who spoke, in the order they actually spoke — the order nobody wrote down. */
    private static String roll(AgenticScope scope) {
        var turns = scope.agentInvocations().stream()
                .filter(i -> List.of("Sniff", "Zoom", "Dig").contains(i.agentName())).toList();
        String order = turns.stream().map(i -> i.agentName()).collect(joining(" → "));
        String said = turns.stream()
                .map(i -> "- **" + i.agentName() + "** — " + String.valueOf(i.output()).strip())
                .collect(joining("\n"));
        return (goatFound(scope) ? "**Goat found.**" : "**Twenty turns, and still no goat.**")
                + " Order of play: " + order + " — nobody chose it.\n\n" + said;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // No leader in the picture, because there is none. The arrows are LISTENING, labelled
        // with the pin that travels: one scent report goes to two pups at once, and both answers
        // come back to Sniff. Every pup reaches the exit — any of them may find the goat.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "mission", "input", 0).withSub("nobody in charge"),
                        node("sniff", "Sniff", "agent", 1).withSub("wakes on either report").as("sniff"),
                        node("zoom", "Zoom", "agent", 2).withSub("wakes on Scent").as("zoom"),
                        node("dig", "Dig", "agent", 2).withSub("wakes on Scent").as("dig"),
                        node("out", "exitCondition", "join", 3).withSub("'FOUND:' from anyone")),
                List.of(edge("in", "sniff"), edge("in", "zoom"), edge("in", "dig"),
                        edge("sniff", "zoom", "scent"), edge("zoom", "sniff", "clearing"),
                        edge("sniff", "dig"), edge("dig", "sniff", "burrows"),
                        edge("sniff", "out"), edge("zoom", "out", "checked every turn"),
                        edge("dig", "out")));
        return new PatternDef("p2p", "Peer-to-Peer", "planner",
                "The Mayor's goat is lost in the corn maze. Three Rangers go in, and nobody is in "
                        + "charge — not even Zao, who is outside, eating corn.",
                null,
                "Peers that trigger each other through shared state, with no coordinator. "
                        + "`P2PPlanner` runs every pup whose input pins are all on the board, and "
                        + "**re-runs a pup whenever one of its pins changes**. So Sniff's scent "
                        + "report wakes Zoom AND Dig at the same moment — one pin, two listeners — "
                        + "and each of their reports wakes Sniff again. Watch the Run events: "
                        + "Sniff, then Zoom and Dig together, then whoever's pins just changed — "
                        + "an order nobody wrote, which is the difference from a loop. It ends when anyone pins "
                        + "`FOUND:`.",
                "Every change wakes every listener, so peers are chatty: when Zoom's and Dig's "
                        + "reports land a moment apart, Sniff fires once for each — budget "
                        + "invocations generously. No hierarchy, so it needs a solid exit predicate — and one that cannot be false "
                        + "ends the search with no goat (ask whether a pin EXISTS and it is true "
                        + "after the first word; read the content). Two more traps, both about who "
                        + "listens to what: a pup activates only once EVERY pin it reads exists, so "
                        + "seed what the first pup needs or nobody moves; and a pup that reads its "
                        + "own pin wakes itself up for ever.",
                topo,
                "Paws up, Rangers! The Mayor's goat, Gertrude, has wandered into the giant corn "
                        + "maze at Hillside Farm. She was last heard bleating somewhere in the middle.",
                P2pPattern::run);
    }
}
