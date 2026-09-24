package dev.devoxx.dashboard.demos._15_bdi;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Fed;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Out;
import dev.devoxx.dashboard.demos._15_bdi.Keys.Session;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>BDI</b> demo — priorities and preconditions decide the order, not the declaration order.
 */
public final class BdiPattern {

    private BdiPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var out = AgenticServices.agentBuilder(GardenLeave.class)
                .chatModel(model)
                .name("GardenLeave")
                .outputKey(Out.class)
                .build();
        var fed = AgenticServices.agentBuilder(FirstBytes.class)
                .chatModel(model)
                .name("FirstBytes")
                .outputKey(Fed.class)
                .build();
        var train = AgenticServices.agentBuilder(HelloWorld.class)
                .chatModel(model)
                .name("HelloWorld")
                .outputKey(Session.class)
                .build();
        // Priorities, not declaration order: shuffle these three and the behaviour is the
        // same. Everyone knows a puppy goes out before he is fed, so the room can check it.
        List<Desire> desires = List.of(
                Desire.of("out-first", 30, s -> true, s -> s.hasState(Out.class),
                        GardenLeave.class),
                Desire.of("then-feed", 20,
                        s -> s.hasState(Out.class), s -> s.hasState(Fed.class),
                        FirstBytes.class),
                Desire.of("then-teach", 5,
                        s -> s.hasState(Out.class) && s.hasState(Fed.class),
                        s -> s.hasState(Session.class), HelloWorld.class));
        FirstHour app = AgenticServices.plannerBuilder(FirstHour.class)
                .subAgents(out, fed, train)
                .planner(() -> new BDIPlanner(desires))
                .outputKey(Session.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        // Named for what they hold rather than for their keys: `out` and `fed` are already
        // the agents' variables a few lines up.
        String wentOut = requireNonNullElse(scope.readState(Out.class), "");
        String wasFed = requireNonNullElse(scope.readState(Fed.class), "");
        String taught = requireNonNullElse(scope.readState(Session.class), "");
        return "**Out first** — " + wentOut
                + "\n\n**Then fed** — " + wasFed
                + "\n\n**Then taught** — " + taught;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "first hour", "input"),
                        node("out", "GardenLeave", "agent").withSub("desire · priority 30"),
                        node("fed", "FirstBytes", "agent").withSub("desire · priority 20"),
                        node("train", "HelloWorld", "agent").withSub("desire · priority 5")),
                // The training session is gated on BOTH of the others, which is what makes this a
                // DAG of desires rather than a chain: 'needs' labels are preconditions, not
                // hand-offs.
                List.of(edge("in", "out"),
                        edge("out", "fed", "needs been out"),
                        edge("out", "train", "needs been out"),
                        edge("fed", "train", "needs fed")));
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "Think back to his first hour here. Eight weeks old, forty minutes in the "
                        + "car, three needs at once. Get the order wrong and you mop.",
                null,
                "The agent pursues prioritised desires, always acting on the highest-priority one "
                        + "that is achievable and not yet met. The puppy's first hour: out ranks "
                        + "food, food ranks training — and that is declared as a priority, not "
                        + "wired as an order.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a desire that can never be satisfied stalls the whole plan.",
                topo,
                "the puppy has just come home — eight weeks old, first hour in the house, forty "
                        + "minutes in the car and not one of them spent asleep. He has not been "
                        + "out since the breeder's",
                BdiPattern::run);
    }
}
