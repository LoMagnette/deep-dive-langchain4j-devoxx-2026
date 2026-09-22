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
        // Every box says what it NEEDS, and the goal box says they were registered backwards
        // — otherwise this is pixel-for-pixel the sequential demo's diagram.
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input").withSub("registered: bikes first"),
                        node("hoover", "NotTheHoover", "agent").withSub("needs nothing"),
                        node("children", "NotTheChildren", "agent").withSub("needs 'Hoover'"),
                        node("cyclists", "NotTheCyclists", "agent").withSub("needs 'Children'")),
                List.of(edge("in", "hoover"),
                        edge("hoover", "children", "writes 'Hoover'"),
                        edge("children", "cyclists", "writes 'Children'")));

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
