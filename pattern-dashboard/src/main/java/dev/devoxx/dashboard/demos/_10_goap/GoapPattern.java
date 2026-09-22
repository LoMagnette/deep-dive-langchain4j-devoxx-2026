package dev.devoxx.dashboard.demos._10_goap;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._10_goap.Keys.Children;
import dev.devoxx.dashboard.demos._10_goap.Keys.Cyclists;
import dev.devoxx.dashboard.demos._10_goap.Keys.Goal;
import dev.devoxx.dashboard.demos._10_goap.Keys.Hoover;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>GOAP</b> demo — the planner works out the order from the declared inputs and outputs.
 */
public final class GoapPattern {

    private GoapPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var hoover = AgenticServices.agentBuilder(NotTheHoover.class)
                .chatModel(model)
                .name("NotTheHoover")
                .outputKey(Hoover.class)
                .build();
        var children = AgenticServices.agentBuilder(NotTheChildren.class)
                .chatModel(model)
                .name("NotTheChildren")
                .outputKey(Children.class)
                .build();
        var cyclists = AgenticServices.agentBuilder(NotTheCyclists.class)
                .chatModel(model)
                .name("NotTheCyclists")
                .outputKey(Cyclists.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                // Registered BACKWARDS on purpose, and it still runs hoover → children →
                // cyclists: the order comes from the I/O keys, not from the order you typed.
                .subAgents(cyclists, children, hoover)
                .planner(GoalOrientedPlanner::new)
                .outputKey(Cyclists.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Goal().name(), input));
        String hooverText = requireNonNullElse(r.agenticScope().readState(Hoover.class), "");
        String childrenText = requireNonNullElse(r.agenticScope().readState(Children.class), "");
        String cyclistText = requireNonNullElse(r.agenticScope().readState(Cyclists.class), "");
        return "**The hoover** — " + hooverText
                + "\n\n**The children** — " + childrenText
                + "\n\n**The cyclists** — " + cyclistText;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // NO ARROWS BETWEEN THE AGENTS, and that is the entire design of this diagram. Drawn
        // as goal → hoover → children → cyclists it was pixel-for-pixel the sequential demo:
        // three boxes wired nose to tail, which is a picture of a path somebody typed. The
        // sub-lines said "needs 'Hoover'" underneath arrows that had already claimed the order,
        // so the caption was arguing with the drawing and the drawing wins.
        //
        // What actually happens: you hand the planner a BAG of agents — here in registration
        // order, which is backwards — and it searches for a chain from what each one needs to
        // what each one writes. Nobody connected them. So the agents sit in one column in the
        // order they were declared, the planner fans out to them, and the arrows carry the
        // positions it DERIVED: 3rd, 2nd, 1st, reading down. That mismatch between the order
        // they are listed in and the order they run in is the pattern, and now it is the first
        // thing you see rather than a line of small print.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "goal", "input", 0),
                        node("plan", "GoalOrientedPlanner", "planner", 1)
                                .withSub("the order is an OUTPUT"),
                        node("cyclists", "NotTheCyclists", "agent", 2)
                                .withSub("needs 'Children'"),
                        node("children", "NotTheChildren", "agent", 2)
                                .withSub("needs 'Hoover'"),
                        node("hoover", "NotTheHoover", "agent", 2)
                                .withSub("needs nothing")),
                List.of(edge("in", "plan", "3 agents, unordered"),
                        edge("plan", "cyclists", "runs 3rd"),
                        edge("plan", "children", "runs 2nd"),
                        edge("plan", "hoover", "runs 1st")));

        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "He is a cattle dog with no cattle, so he has improvised. The hoover has "
                        + "been gathered. The children have been gathered.",
                null,
                "The planner orders agents automatically by matching each output to the next "
                        + "input. Nobody has to be told this order: you can call him off a hoover "
                        + "long before you can call him off a child, and off a child long before "
                        + "you can call him off a cyclist — loud but stationary, then fast but "
                        + "biddable, then fast and silent and gone. So there is genuinely an "
                        + "order to discover, and you can see it was discovered rather than typed.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable — and "
                        + "the failure is silence, not an error.",
                topo,
                "Zao has decided the hoover is livestock. So are the children. So, increasingly, "
                        + "are cyclists. Teach him that none of them are.",
                GoapPattern::run);
    }
}
