package dev.devoxx.dashboard.demos._10_goap;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._10_goap.Keys.Chair;
import dev.devoxx.dashboard.demos._10_goap.Keys.Decoy;
import dev.devoxx.dashboard.demos._10_goap.Keys.Sausage;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
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
        var decoy = AgenticServices.agentBuilder(DoorbellDecoy.class)
                .chatModel(model)
                .name("DoorbellDecoy")
                .outputKey(Decoy.class)
                .build();
        var pusher = AgenticServices.agentBuilder(ChairPusher.class)
                .chatModel(model)
                .name("ChairPusher")
                .outputKey(Chair.class)
                .build();
        var surfer = AgenticServices.agentBuilder(CounterSurfer.class)
                .chatModel(model)
                .name("CounterSurfer")
                .outputKey(Sausage.class)
                .build();

        GoapMission app = AgenticServices.plannerBuilder(GoapMission.class)
                .subAgents(surfer, pusher, decoy)
                .planner(GoalOrientedPlanner::new)
                .outputKey(Sausage.class)
                .listener(listener)
                .build();

        var r = app.invoke(input);
        String decoyText = requireNonNullElse(r.agenticScope().readState(Decoy.class), "");
        String chairText = requireNonNullElse(r.agenticScope().readState(Chair.class), "");
        String sausageText = requireNonNullElse(r.agenticScope().readState(Sausage.class), "");
        return "**The decoy** — " + decoyText
                + "\n\n**The chair** — " + chairText
                + "\n\n**The sausage** — " + sausageText;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // NO ARROWS BETWEEN THE AGENTS, and that is the entire design of this diagram. Drawn
        // as goal → decoy → chair → sausage it was pixel-for-pixel the sequential demo:
        // three boxes wired nose to tail, which is a picture of a path somebody typed. The
        // sub-lines said "needs 'Decoy'" underneath arrows that had already claimed the order,
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
                        // The role name on top, the dog and what it needs underneath: the
                        // breed is who, the key is why it cannot go first.
                        node("surfer", "CounterSurfer", "agent", 2)
                                .withSub("Corgi · needs 'Chair'"),
                        node("pusher", "ChairPusher", "agent", 2)
                                .withSub("Bulldog · needs 'Decoy'"),
                        node("decoy", "DoorbellDecoy", "agent", 2)
                                .withSub("Beagle · needs nothing")),
                List.of(edge("in", "plan", "3 agents, unordered"),
                        edge("plan", "surfer", "runs 3rd"),
                        edge("plan", "pusher", "runs 2nd"),
                        edge("plan", "decoy", "runs 1st")));

        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "A sausage on the counter. The Corgi can't reach it, the chair is loud, and "
                        + "the human is still in the kitchen.",
                null,
                "The planner orders agents automatically by matching each output to the next "
                        + "input. Nobody has to be told this order: the chair scrapes on the "
                        + "tiles, so it cannot move while the human is in the room, and the Corgi "
                        + "cannot reach the counter without it. Decoy, then chair, then Corgi — "
                        + "and the agents are handed to the planner the other way round, so you "
                        + "can see the order was discovered rather than typed.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable — and "
                        + "the failure is silence, not an error. Drop the Bulldog from "
                        + "`subAgents(...)` and the sausage is unreachable: no exception, just "
                        + "a pack sitting by the counter, looking at it.",
                topo,
                "Zao's orders: the sausage on the kitchen counter, in his bowl, before the human "
                        + "notices. The pack has three talents and no plan.",
                GoapPattern::run);
    }
}
