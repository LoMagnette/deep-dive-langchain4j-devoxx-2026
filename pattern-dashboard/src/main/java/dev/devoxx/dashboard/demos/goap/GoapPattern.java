package dev.devoxx.dashboard.demos.goap;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.goap.Keys.Garden;
import dev.devoxx.dashboard.demos.goap.Keys.Goal;
import dev.devoxx.dashboard.demos.goap.Keys.Indoor;
import dev.devoxx.dashboard.demos.goap.Keys.Park;
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
        var indoor = AgenticServices.agentBuilder(IndoorRecall.class)
                .chatModel(model)
                .name("IndoorRecall")
                .outputKey(Indoor.class)
                .build();
        var garden = AgenticServices.agentBuilder(GardenRecall.class)
                .chatModel(model)
                .name("GardenRecall")
                .outputKey(Garden.class)
                .build();
        var park = AgenticServices.agentBuilder(ParkRecall.class)
                .chatModel(model)
                .name("ParkRecall")
                .outputKey(Park.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                // Registered BACKWARDS on purpose, and it still runs indoor → garden → park.
                // That is the whole pattern: the order comes from the I/O keys (ParkRecall
                // needs 'Garden', GardenRecall needs 'Indoor'), not from the order you
                // happened to type. Say this out loud on stage — it is the one moment where
                // GOAP is visibly not a sequence with extra ceremony.
                .subAgents(park, garden, indoor)
                .planner(GoalOrientedPlanner::new)
                .outputKey(Park.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Goal().name(), input));
        String indoorText = requireNonNullElse(r.agenticScope().readState(Indoor.class), "");
        String gardenText = requireNonNullElse(r.agenticScope().readState(Garden.class), "");
        String parkText = requireNonNullElse(r.agenticScope().readState(Park.class), "");
        return "**Indoors** — " + indoorText
                + "\n\n**Garden** — " + gardenText
                + "\n\n**Park** — " + parkText;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // Every box says what it NEEDS, because that is the only thing distinguishing this
        // picture from the sequential demo's. The planner was handed these three backwards; the
        // arrows are what it worked out from the keys, not an order anybody typed — and the
        // goal box says so, because otherwise the reader has to be TOLD the order was derived,
        // and not having to be told is what the picture is for.
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input").withSub("registered: park first"),
                        node("indoor", "IndoorRecall", "agent").withSub("needs nothing"),
                        node("garden", "GardenRecall", "agent").withSub("needs 'Indoor'"),
                        node("park", "ParkRecall", "agent").withSub("needs 'Garden'")),
                List.of(edge("in", "indoor"),
                        edge("indoor", "garden", "writes 'Indoor'"),
                        edge("garden", "park", "writes 'Garden'")));

        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "Which begins, as everything does, with the recall you never finished "
                        + "teaching him. He comes back indoors. Reliably. Indoors.",
                null,
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
                GoapPattern::run);
    }
}
