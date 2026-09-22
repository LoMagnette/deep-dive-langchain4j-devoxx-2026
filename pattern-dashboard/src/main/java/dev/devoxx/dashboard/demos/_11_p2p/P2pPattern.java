package dev.devoxx.dashboard.demos._11_p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Agreement;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Proposal;
import dev.devoxx.dashboard.demos._11_p2p.Keys.Question;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>peer-to-peer</b> demo — two peers who cannot overrule each other.
 */
public final class P2pPattern {

    private P2pPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var bed = AgenticServices.agentBuilder(TeamOnTheBed.class)
                .chatModel(model)
                .name("TeamOnTheBed")
                .outputKey(Proposal.class)
                .build();
        var floor = AgenticServices.agentBuilder(TeamOnTheFloor.class)
                .chatModel(model)
                .name("TeamOnTheFloor")
                .outputKey(Agreement.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                .subAgents(bed, floor)
                // The exit predicate is the only thing that ends this: neither side can
                // overrule the other, so without it they counter each other forever.
                .planner(() -> new P2PPlanner(10, s -> s.hasState(Agreement.class)))
                .outputKey(Agreement.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Question().name(), input));
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The exit predicate is the fourth box: two peers passing a proposal back and forth
        // with no end drawn is the pattern's danger, not this demo's behaviour.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "question", "input", 0),
                        node("bed", "TeamOnTheBed", "agent", 1).withSub("proposes"),
                        node("floor", "TeamOnTheFloor", "agent", 1)
                                .withSub("counters — or agrees"),
                        node("out", "the rule both keep", "join", 2)
                                .withSub("exit: they agreed")),
                List.of(edge("in", "bed"),
                        edge("bed", "floor", "proposal"),
                        edge("floor", "bed", "counter · up to 10"),
                        edge("floor", "out", "agreement")));
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "And the argument you have been avoiding for a year. The bed. Neither of you "
                        + "outranks the other, which is why this is not a supervisor.",
                null,
                "Peers refine a shared state until an exit condition holds. The case for it: "
                        + "every household has had this argument, and the reason it is not a "
                        + "supervisor is that neither half can overrule the other — so the only "
                        + "way out is a rule both will actually keep.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No hierarchy — needs a solid exit predicate or it never terminates, and "
                        + "\"they'll converge eventually\" is not one.",
                topo,
                "should Zao be allowed to sleep on the bed? He is asleep on the bed.",
                P2pPattern::run);
    }
}
