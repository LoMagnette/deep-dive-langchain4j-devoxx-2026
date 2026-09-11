package dev.devoxx.dashboard.demos.bdi;

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
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;

/**
 * Wiring for the <b>BDI</b> demo — priorities and preconditions decide the order, not the declaration order.
 */
public final class BdiPattern {

    private BdiPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "first hour", "input"),
                        node("out", "ToiletTrip (p30)", "agent"),
                        node("fed", "FirstMeal (p20)", "agent"),
                        node("train", "FirstTraining (p5)", "agent")),
                // The training session is gated on BOTH of the others, which is what makes this a
                // DAG of desires rather than a chain: 'needs' labels are preconditions, not
                // hand-offs.
                List.of(edge("in", "out"),
                        edge("out", "fed", "needs been out"),
                        edge("out", "train", "needs been out"),
                        edge("fed", "train", "needs fed")));
        Runner runner = (model, input, listener) -> {
            var out = AgenticServices.agentBuilder(ToiletTrip.class)
                    .chatModel(model)
                    .name("ToiletTrip")
                    .outputKey("out")
                    .build();
            var fed = AgenticServices.agentBuilder(FirstMeal.class)
                    .chatModel(model)
                    .name("FirstMeal")
                    .outputKey("fed")
                    .build();
            var train = AgenticServices.agentBuilder(FirstTraining.class)
                    .chatModel(model)
                    .name("FirstTraining")
                    .outputKey("session")
                    .build();
            // Priorities, not order. Nobody needs telling that a puppy goes out before he is fed
            // and long before he is taught anything — so the room can see the planner making the
            // right call instead of taking it on trust. Shuffle these three declarations and the
            // behaviour does not change, which is the point of BDI and impossible to show with
            // two agents in the only order they could ever have run.
            List<Desire> desires = List.of(
                    Desire.of("out-first", 30, s -> true, s -> s.hasState("out"),
                            ToiletTrip.class),
                    Desire.of("then-feed", 20, s -> s.hasState("out"), s -> s.hasState("fed"),
                            FirstMeal.class),
                    Desire.of("then-teach", 5,
                            s -> s.hasState("out") && s.hasState("fed"),
                            s -> s.hasState("session"), FirstTraining.class));
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(out, fed, train)
                    .planner(() -> new BDIPlanner(desires))
                    .outputKey("session")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("hour", input));
            var scope = r.agenticScope();
            if (scope == null) {
                return String.valueOf(r.result());
            }
            return "**Out first** — " + scope.readState("out", "")
                    + "\n\n**Then fed** — " + scope.readState("fed", "")
                    + "\n\n**Then taught** — " + scope.readState("session", "");
        };
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "The agent pursues prioritised desires, always acting on the highest-priority one "
                        + "that is achievable and not yet met. The puppy's first hour: out ranks "
                        + "food, food ranks training — and that is declared as a priority, not "
                        + "wired as an order.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable and satisfied predicates are hard to get "
                        + "right, and a desire that can never be satisfied stalls the whole plan.",
                topo,
                "the puppy has just come home — eight weeks old, first hour in the house, "
                        + "he's been in the car for forty minutes",
                runner);
    }
}
