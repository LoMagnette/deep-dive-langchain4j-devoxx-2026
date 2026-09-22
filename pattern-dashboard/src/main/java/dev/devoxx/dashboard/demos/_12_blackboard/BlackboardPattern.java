package dev.devoxx.dashboard.demos._12_blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Causes;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Home;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Problem;
import dev.devoxx.dashboard.demos._12_blackboard.Keys.Routine;
import dev.devoxx.dashboard.demos._04_parallel.Keys.Walks;
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
        Topology.Graph topo = graph("star",
                // The only pattern that still draws the shared state: here it is not plumbing,
                // it is the pattern. Every other topology dropped its AgenticScope sink — it was
                // the same box in all 13 diagrams, and the scope now has its own tab.
                //
                // Four identical satellites said nothing about where the problem comes from,
                // why the order is free, or how the run ever stops. The sub-lines carry all
                // three: the board holds the problem, the note-takers each need only that (so
                // any of them can go first), and the lead needs all three and ends it.
                List.of(node("board", "The board", "board").withSub("the problem + every note"),
                        node("walks", "WalkNotes", "agent").withSub("needs only the problem"),
                        node("routine", "RoutineNotes", "agent").withSub("needs only the problem"),
                        node("home", "HomeNotes", "agent").withSub("needs only the problem"),
                        node("lead", "TrainerLead", "agent").withSub("needs all three, ends it")),
                // Contributors read the board as well as write to it — that mutual dependency is
                // why the pattern needs a conflict-resolution strategy at all.
                List.of(edge("walks", "board", "exercise"), edge("board", "walks"),
                        edge("routine", "board", "changes"), edge("board", "routine"),
                        edge("home", "board", "the house"), edge("board", "home"),
                        edge("lead", "board", "ranked causes"), edge("board", "lead")));
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
                        + "complained twice. He never used to. Nothing has changed except my new "
                        + "shift and we moved his bed under the front window.",
                BlackboardPattern::run);
    }
}
