package dev.devoxx.dashboard.demos.p2p;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;

/**
 * Wiring for the <b>peer-to-peer</b> demo — two peers who cannot overrule each other.
 */
public final class P2pPattern {

    private P2pPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "question", "input"),
                        node("bed", "TeamOnTheBed", "agent"),
                        node("floor", "TeamOnTheFloor", "agent")),
                List.of(edge("in", "bed"),
                        edge("bed", "floor", "proposal"),
                        edge("floor", "bed", "counter")));
        Runner runner = (model, input, listener) -> {
            var bed = AgenticServices.agentBuilder(TeamOnTheBed.class)
                    .chatModel(model)
                    .name("TeamOnTheBed")
                    .outputKey("proposal")
                    .build();
            var floor = AgenticServices.agentBuilder(TeamOnTheFloor.class)
                    .chatModel(model)
                    .name("TeamOnTheFloor")
                    .outputKey("agreement")
                    .build();
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(bed, floor)
                    // The exit predicate is the only thing that ends this: neither side can
                    // overrule the other, so without it they counter each other forever.
                    .planner(() -> new P2PPlanner(10, s -> s.hasState("agreement")))
                    .outputKey("agreement")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("question", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "Peers refine a shared state until an exit condition holds. The case for it: "
                        + "every household has had this argument, and the reason it is not a "
                        + "supervisor is that neither half can overrule the other — so the only "
                        + "way out is a rule both will actually keep.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No hierarchy — needs a solid exit predicate or it never terminates, and "
                        + "\"they'll converge eventually\" is not one.",
                topo,
                "should Zao be allowed to sleep on the bed?",
                runner);
    }
}
