package dev.devoxx.dashboard.demos._12_blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Causes;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Home;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Problem;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Routine;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>blackboard</b> demo — three kinds of knowledge, contributed in any order.
 */
public final class BlackboardPattern {

    private BlackboardPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The three note-takers read ONLY 'problem', so any of them can go first and the
        // board accumulates three different KINDS of knowledge. Chain them — each reading the
        // last one's output — and you have a sequence wearing a blackboard's coat.
        var walks = AgenticServices.agentBuilder(WalkNotes.class)
                .chatModel(model)
                .name("WalkNotes")
                .outputKey(Walks.class)
                .build();
        var routine = AgenticServices.agentBuilder(RoutineNotes.class)
                .chatModel(model)
                .name("RoutineNotes")
                .outputKey(Routine.class)
                .build();
        var home = AgenticServices.agentBuilder(HomeNotes.class)
                .chatModel(model)
                .name("HomeNotes")
                .outputKey(Home.class)
                .build();
        var lead = AgenticServices.agentBuilder(TrainerLead.class)
                .chatModel(model)
                .name("TrainerLead")
                .outputKey(Causes.class)
                .build();
        Predicate<AgenticScope> goal = s -> s.hasState(Causes.class);
        UntypedAgent app = AgenticServices.plannerBuilder()
                .subAgents(walks, routine, home, lead)
                .planner(() -> new BlackboardPlanner(goal,
                        ConflictResolutionStrategy.declarationOrder()))
                .outputKey(Causes.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Problem().name(), input));
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // This was a `star`: the board in the middle with all four agents evenly round it. The
        // circle got one thing right — the three note-takers genuinely have no order — and
        // three things wrong, each of which this diagram had already been fixed for elsewhere.
        // The problem arrived from nowhere; the run ended nowhere; and TrainerLead, which can
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
                List.of(node("in", "the problem", "input", 0),
                        node("board", "The board", "board", 1)
                                .withSub("problem + every note"),
                        node("walks", "WalkNotes", "agent", 2)
                                .withSub("needs only the problem"),
                        node("routine", "RoutineNotes", "agent", 2)
                                .withSub("needs only the problem"),
                        node("home", "HomeNotes", "agent", 2)
                                .withSub("needs only the problem"),
                        // Three identical sub-lines are the point: three boxes that say the
                        // same thing are three agents with nothing to tell them apart, which
                        // is exactly why any of them can go first. The fourth reads
                        // differently because it IS different.
                        node("lead", "TrainerLead", "agent", 3)
                                .withSub("needs all three · last"),
                        node("out", "ranked causes", "join", 4)
                                .withSub("the goal state")),
                // Contributors read the board as well as write to it — that mutual dependency
                // is why the pattern needs a conflict-resolution strategy at all. Only the
                // write half is labelled, as with the supervisor's pair: of the two it is the
                // contribution that carries the mechanism.
                List.of(edge("in", "board"),
                        edge("board", "walks"), edge("walks", "board", "exercise"),
                        edge("board", "routine"), edge("routine", "board", "the shift"),
                        edge("board", "home"), edge("home", "board", "the window"),
                        // Skips the contributors' column, so it arcs over them — which is what
                        // "reads the whole board" looks like when it is drawn rather than said.
                        edge("board", "lead", "all three notes"),
                        edge("lead", "out", "ranked causes")));
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "Meanwhile the neighbour has complained twice. He barks all day now. Nothing "
                        + "has changed, except everything that has changed.",
                null,
                "Contributors read and write a shared board until a goal state exists. This is "
                        + "debugging, which is what a blackboard is for: barking while you are "
                        + "out is an exercise question, a what-changed question and a "
                        + "what-can-he-see question until the board says which one it is.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy. And "
                        + "be honest about whether your contributors really are order-independent.",
                topo,
                "he's started barking all day while we're at work and the neighbour has "
                        + "complained twice. He never used to. Nothing has changed — except my "
                        + "new shift, and we moved his bed under the front window, and he gets a "
                        + "shorter walk now. But nothing has changed.",
                BlackboardPattern::run);
    }
}
