package dev.devoxx.dashboard.demos._12_blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Culprit;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.ScentClue;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.TunnelClue;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 12</b> — three kinds of clue, pinned in any order, then a ruling.
 */
public final class BlackboardPattern {

    private BlackboardPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        // The three clue-finders read ONLY the mission, so any of them can go first. Chain them
        // — each reading the last one's clue — and you have a sequence wearing a board's coat.
        var sniff = AgenticServices.agentBuilder(SniffTrails.class)
                .chatModel(model)
                .name("Sniff")
                .outputKey(ScentClue.class)
                .build();
        var dig = AgenticServices.agentBuilder(DigTunnels.class)
                .chatModel(model)
                .name("Dig")
                .outputKey(TunnelClue.class)
                .build();
        var bolt = new BoltCameras();
        var zao = AgenticServices.agentBuilder(ZaoNamesTheCulprit.class)
                .chatModel(model)
                .name("Zao")
                .outputKey(Culprit.class)
                .build();
        Predicate<AgenticScope> solved = s -> s.hasState(Culprit.class);
        Investigation app = AgenticServices.plannerBuilder(Investigation.class)
                .subAgents(sniff, dig, bolt, zao)
                .planner(() -> new BlackboardPlanner(solved,
                        ConflictResolutionStrategy.declarationOrder()))
                .outputKey(Culprit.class)
                .listener(listener)
                .build();
        return app.invoke(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Columns, with the board as its own dashed box — the one diagram that draws the scope,
        // because here the shared state IS the pattern. The three clue-finders share a column,
        // which is how a picture says "no order"; Zao has his own, after them; and the way in
        // and the way out are both drawn.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the crime", "input", 0),
                        node("board", "Pup Board", "board", 1).withSub("every clue pinned here"),
                        node("sniff", "Sniff", "agent", 2).withSub("needs only the crime").as("sniff"),
                        node("dig", "Dig", "agent", 2).withSub("needs only the crime").as("dig"),
                        node("bolt", "Bolt", "code", 2).withSub("needs only the crime").as("bolt"),
                        // Three identical sub-lines are the point: nothing tells the three apart,
                        // which is exactly why any of them can go first.
                        node("zao", "Zao", "agent", 3).withSub("needs all three · last").as("zao"),
                        node("out", "culprit", "join", 4).withSub("the goal state")),
                List.of(edge("in", "board"),
                        edge("board", "sniff"), edge("sniff", "board", "the scent"),
                        edge("board", "dig"), edge("dig", "board", "paw prints"),
                        edge("board", "bolt"), edge("bolt", "board", "timestamps"),
                        edge("board", "zao", "all three clues"),
                        edge("zao", "out", "names the culprit")));
        return new PatternDef("blackboard", "Blackboard", "planner",
                "The town's sausages are gone from the butcher's. There is a sausage crumb in "
                        + "Zao's beard. Zao would like it known that this proves nothing.",
                null,
                "Contributors read and write a shared board until a goal state exists — the "
                        + "AgenticScope is the Pup Board. Sniff pins the scent, Dig the paw "
                        + "prints, Bolt the camera timestamps, in whatever order the planner "
                        + "picks, and Zao rules once all three are up. Three different KINDS of "
                        + "evidence, and only the board together clears the obvious suspect.",
                "Shared mutable state invites conflicts — pick a conflict-resolution strategy. "
                        + "And be honest about whether your contributors really are "
                        + "order-independent: give Dig a clue Sniff writes, and the board has an "
                        + "order again.",
                topo,
                "Paws up, Rangers! The Great Sausage Heist: every sausage in the butcher's "
                        + "window is gone, the door was locked, and a sausage crumb has been found "
                        + "in Zao's beard.",
                BlackboardPattern::run);
    }
}
