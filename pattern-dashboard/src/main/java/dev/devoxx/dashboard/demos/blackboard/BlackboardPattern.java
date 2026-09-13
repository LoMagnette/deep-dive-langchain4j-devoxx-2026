package dev.devoxx.dashboard.demos.blackboard;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Wiring for the <b>blackboard</b> demo — three kinds of knowledge, contributed in any order.
 */
public final class BlackboardPattern {

    private BlackboardPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("star",
                // The only pattern that still draws the shared state: here it is not plumbing,
                // it is the pattern. Every other topology dropped its AgenticScope sink — it was
                // the same box in all 13 diagrams, and the scope now has its own tab.
                List.of(node("board", "The board", "board"),
                        node("walks", "WalkNotes", "agent"),
                        node("routine", "RoutineNotes", "agent"),
                        node("home", "HomeNotes", "agent"),
                        node("lead", "TrainerLead", "agent")),
                // Contributors read the board as well as write to it — that mutual dependency is
                // why the pattern needs a conflict-resolution strategy at all.
                List.of(edge("walks", "board", "exercise"), edge("board", "walks"),
                        edge("routine", "board", "changes"), edge("board", "routine"),
                        edge("home", "board", "the house"), edge("board", "home"),
                        edge("lead", "board", "ranked causes"), edge("board", "lead")));
        Runner runner = (model, input, listener) -> {
            // The three note-takers read ONLY 'problem', so any of them can go first and the
            // board accumulates three different KINDS of knowledge. Chain them instead — each
            // reading the last one's output — and you have written a sequence wearing a
            // blackboard's coat, which is what this demo used to be.
            var walks = AgenticServices.agentBuilder(WalkNotes.class)
                    .chatModel(model)
                    .name("WalkNotes")
                    .outputKey("walks")
                    .build();
            var routine = AgenticServices.agentBuilder(RoutineNotes.class)
                    .chatModel(model)
                    .name("RoutineNotes")
                    .outputKey("routine")
                    .build();
            var home = AgenticServices.agentBuilder(HomeNotes.class)
                    .chatModel(model)
                    .name("HomeNotes")
                    .outputKey("home")
                    .build();
            var lead = AgenticServices.agentBuilder(TrainerLead.class)
                    .chatModel(model)
                    .name("TrainerLead")
                    .outputKey("causes")
                    .build();
            Predicate<AgenticScope> goal = s -> s.hasState("causes");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(walks, routine, home, lead)
                    .planner(() -> new BlackboardPlanner(goal,
                            ConflictResolutionStrategy.declarationOrder()))
                    .outputKey("causes")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("problem", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                // The beat this demo plays in the running narration.
                "Meanwhile the neighbour has complained twice. He barks all day now and "
                        + "nobody knows why.",
                // What this demo inherits from the ones before it.
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
                runner);
    }
}
