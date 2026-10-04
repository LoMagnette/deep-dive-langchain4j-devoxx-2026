package dev.devoxx.dashboard.demos._10_goap;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.firstNumber;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.Ladder;
import dev.devoxx.dashboard.demos._08_nonaiagent.Keys.LadderLength;
import dev.devoxx.dashboard.demos._08_nonaiagent.Rivet;
import dev.devoxx.dashboard.demos._10_goap.Keys.CatSafe;
import dev.devoxx.dashboard.demos._10_goap.Keys.LadderSecured;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 10</b> — the planner derives the order from what each Ranger needs.
 */
public final class GoapPattern {

    private GoapPattern() {
    }

    static String run(ChatModel model, String input, StreamingListener listener) {
        var r = CurrentRun.with(listener, () ->
                AgenticServices.createAgenticSystem(GoapMission.class, model).invoke(firstNumber(input, 12.0)));
        var s = r.agenticScope();
        return "**Rivet** — the ladder must be " + s.readState(LadderLength.class) + " m"
                + "\n\n**Zoom** — " + requireNonNullElse(s.readState(Ladder.class), "")
                + "\n\n**Dig** — " + requireNonNullElse(s.readState(LadderSecured.class), "")
                + "\n\n**Doc** — " + requireNonNullElse(s.readState(CatSafe.class), "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // NO ARROWS BETWEEN THE RANGERS, and that is the entire design of this diagram. Drawn
        // nose to tail it would be the sequential demo: a path somebody typed. Here you hand the
        // planner a bag of agents in a scrambled order and it searches back from the goal, so
        // the Rangers sit in one column in the order they were REGISTERED and the arrows carry
        // the positions the planner DERIVED. That mismatch is the pattern.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "towerHeight", "input", 0).withSub("goal: catSafe"),
                        node("plan", "GoalOrientedPlanner", "planner", 1)
                                .withSub("works back from the goal"),
                        node("doc", "Doc", "agent", 2).withSub("needs 'LadderSecured'").as("doc"),
                        node("rivet", "Rivet", "code", 2).withSub("needs 'Height'").as("rivet"),
                        node("dig", "Dig", "agent", 2).withSub("needs 'Ladder'").as("dig"),
                        node("zoom", "Zoom", "agent", 2).withSub("needs 'LadderLength'").as("zoom")),
                List.of(edge("in", "plan", "4 Rangers, scrambled"),
                        edge("plan", "doc", "runs 4th"),
                        edge("plan", "rivet", "runs 1st"),
                        edge("plan", "dig", "runs 3rd"),
                        edge("plan", "zoom", "runs 2nd")));
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "planner",
                "Marmalade is stuck on the water tower. Again. The Rangers are registered in the "
                        + "wrong order, and the planner does not care.",
                "Mission 8's Rivet and Zoom, unchanged, plus Dig and Doc.",
                "Goal = `CatSafe`. Each Ranger declares what it needs and what it pins, and the "
                        + "planner works backwards: Doc climbs (needs `LadderSecured`) ← Dig "
                        + "steadies the ladder (needs `Ladder`) ← Zoom fetches it (needs "
                        + "`LadderLength`) ← Rivet computes it (needs `Height`). The Rangers are "
                        + "handed over scrambled — Doc, Rivet, Dig, Zoom — and they still run "
                        + "Rivet, Zoom, Dig, Doc. **The order is an output.**",
                "Only as good as the declared keys: a missing link makes the goal unreachable, and "
                        + "the failure is silence, not an error. Take Dig out of `subAgents = {...}` "
                        + "and nobody can ever secure the ladder, so Doc never climbs — no "
                        + "exception, just Marmalade, still on the tower, looking smug.",
                topo,
                "Paws up, Rangers! Marmalade is stuck on top of the water tower — 12 metres up — and "
                        + "is yowling at the whole of Barkville.",
                GoapPattern::run)
                .gist("Name the goal; the planner derives the order from what each needs.");
    }
}
