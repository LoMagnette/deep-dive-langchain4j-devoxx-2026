package dev.devoxx.dashboard.demos._12_blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Alibis;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Ruling;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Scene;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Trail;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>blackboard</b> demo — three kinds of evidence, contributed in any order.
 */
public final class BlackboardPattern {

    private BlackboardPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The three investigators read ONLY 'crime', so any of them can go first and the
        // board accumulates three different KINDS of evidence. Chain them — each reading the
        // last one's output — and you have a sequence wearing a blackboard's coat.
        var bloodhound = AgenticServices.agentBuilder(ScentTrail.class)
                .chatModel(model)
                .name("ScentTrail")
                .outputKey(Trail.class)
                .build();
        var collie = AgenticServices.agentBuilder(AlibiCheck.class)
                .chatModel(model)
                .name("AlibiCheck")
                .outputKey(Alibis.class)
                .build();
        var shepherd = AgenticServices.agentBuilder(CrimeScene.class)
                .chatModel(model)
                .name("CrimeScene")
                .outputKey(Scene.class)
                .build();
        var zao = AgenticServices.agentBuilder(PackLeader.class)
                .chatModel(model)
                .name("PackLeader")
                .outputKey(Ruling.class)
                .build();
        Predicate<AgenticScope> goal = s -> s.hasState(Ruling.class);
        Investigation app = AgenticServices.plannerBuilder(Investigation.class)
                .subAgents(bloodhound, collie, shepherd, zao)
                .planner(() -> new BlackboardPlanner(goal,
                        ConflictResolutionStrategy.declarationOrder()))
                .outputKey(Ruling.class)
                .listener(listener)
                .build();
        return app.invoke(input);
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // This was a `star`: the board in the middle with all four agents evenly round it. The
        // circle got one thing right — the three note-takers genuinely have no order — and
        // three things wrong, each of which this diagram had already been fixed for elsewhere.
        // The problem arrived from nowhere; the run ended nowhere; and the lead, which can
        // only act once all three notes exist and is the thing that ENDS the run, was drawn as
        // a fourth identical satellite. Worse, `star` places satellites at top/right/bottom/
        // left in declaration order, so the one box that must go last sat at the far LEFT,
        // where the eye starts. Eight arrows radiating from one box did not help.
        //
        // Columns instead, with the board kept as its own dashed box because here the shared
        // state really is the pattern — the one diagram in the catalogue that draws the scope.
        // The three peers share a column, which is how a picture says "no order"; the lead has
        // its own, after them; and the way in and the way out are both drawn.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the crime", "input", 0),
                        node("board", "The board", "board", 1)
                                .withSub("crime + every note"),
                        node("trail", "ScentTrail", "agent", 2)
                                .withSub("Bloodhound · crime only"),
                        node("alibis", "AlibiCheck", "agent", 2)
                                .withSub("Collie · crime only"),
                        node("scene", "CrimeScene", "agent", 2)
                                .withSub("Shepherd · crime only"),
                        // Three sub-lines that end the same way are the point: apart from
                        // which dog it is, nothing tells the three apart, which is exactly
                        // why any of them can go first. The fourth reads differently because
                        // it IS different.
                        node("lead", "PackLeader", "agent", 3)
                                .withSub("Zao · needs all three"),
                        node("out", "the culprit", "join", 4)
                                .withSub("the goal state")),
                // Contributors read the board as well as write to it — that mutual dependency
                // is why the pattern needs a conflict-resolution strategy at all. Only the
                // write half is labelled, as with the supervisor's pair: of the two it is the
                // contribution that carries the mechanism.
                List.of(edge("in", "board"),
                        edge("board", "trail"), edge("trail", "board", "the crumbs"),
                        edge("board", "alibis"), edge("alibis", "board", "who fits"),
                        edge("board", "scene"), edge("scene", "board", "the plate"),
                        // Skips the contributors' column, so it arcs over them — which is what
                        // "reads the whole board" looks like when it is drawn rather than said.
                        edge("board", "lead", "all three notes"),
                        edge("lead", "out", "most guilty first")));
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "The birthday cake is gone. The Labrador has cream on his nose. The Labrador "
                        + "does not fit through the dog flap.",
                null,
                "Contributors read and write a shared board until a goal state exists. This is "
                        + "an investigation, which is what a blackboard is for: the trail, the "
                        + "alibis and the scene are three kinds of evidence that can arrive in "
                        + "any order, and nobody can name the culprit until all three are on the "
                        + "board. The obvious suspect is the one with cream on his nose; the "
                        + "board is what gets past him.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy. And "
                        + "be honest about whether your contributors really are order-independent.",
                topo,
                "the birthday cake has gone from the coffee table. The Labrador has cream on "
                        + "his nose and is looking at the floor. There is a trail of crumbs out "
                        + "through the dog flap. The Labrador does not fit through the dog flap. "
                        + "The crumbs stop at the Dachshund, who is asleep on the lawn, and is "
                        + "noticeably rounder than he was this morning.",
                BlackboardPattern::run);
    }
}
