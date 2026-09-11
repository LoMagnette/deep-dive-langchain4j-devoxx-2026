package dev.devoxx.dashboard.demos.goap;

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
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;

/**
 * Wiring for the <b>GOAP</b> demo — the planner works out the order from the declared inputs and outputs.
 */
public final class GoapPattern {

    private GoapPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input"),
                        node("indoor", "IndoorRecall", "agent"),
                        node("garden", "GardenRecall", "agent"),
                        node("park", "ParkRecall", "agent")),
                List.of(edge("in", "indoor"),
                        edge("indoor", "garden", "indoor"),
                        edge("garden", "park", "garden")));
        Runner runner = (model, input, listener) -> {
            var indoor = AgenticServices.agentBuilder(IndoorRecall.class)
                    .chatModel(model)
                    .name("IndoorRecall")
                    .outputKey("indoor")
                    .build();
            var garden = AgenticServices.agentBuilder(GardenRecall.class)
                    .chatModel(model)
                    .name("GardenRecall")
                    .outputKey("garden")
                    .build();
            var park = AgenticServices.agentBuilder(ParkRecall.class)
                    .chatModel(model)
                    .name("ParkRecall")
                    .outputKey("park")
                    .build();
            UntypedAgent app = AgenticServices.plannerBuilder()
                    // Registered BACKWARDS on purpose, and it still runs indoor → garden → park.
                    // That is the whole pattern: the order comes from the I/O keys (ParkRecall
                    // needs 'garden', GardenRecall needs 'indoor'), not from the order you
                    // happened to type. Say this out loud on stage — it is the one moment where
                    // GOAP is visibly not a sequence with extra ceremony.
                    .subAgents(park, garden, indoor)
                    .planner(GoalOrientedPlanner::new)
                    .outputKey("park")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("goal", input));
            return "**Indoors** — " + r.agenticScope().readState("indoor", "")
                    + "\n\n**Garden** — " + r.agenticScope().readState("garden", "")
                    + "\n\n**Park** — " + r.agenticScope().readState("park", "");
        };
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "The planner orders agents automatically by matching each output to the next "
                        + "input. Nobody has to be told this order: recall works indoors before "
                        + "it works in the garden, and in the garden before it works at the park. "
                        + "So there is genuinely an order to discover, and you can see it was "
                        + "discovered rather than typed.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable — and "
                        + "the failure is silence, not an error.",
                topo,
                "teach Zao to come back when he's called, even at the park with other dogs about",
                runner);
    }
}
