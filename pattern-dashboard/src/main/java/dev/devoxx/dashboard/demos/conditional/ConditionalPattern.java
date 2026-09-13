package dev.devoxx.dashboard.demos.conditional;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * Wiring for the <b>conditional routing</b> demo — where sending it to the wrong person is the disaster.
 */
public final class ConditionalPattern {

    private ConditionalPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "worry", "input"),
                        node("router", "WorryRouter", "router"),
                        node("vet", "EmergencyVet", "agent"),
                        node("trainer", "DogTrainer", "agent"),
                        node("care", "EverydayCare", "agent")),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday")));
        Runner runner = (model, input, listener) -> {
            var router = AgenticServices.agentBuilder(WorryRouter.class)
                    .chatModel(model)
                    .name("WorryRouter")
                    .outputKey("category")
                    .build();
            var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                    .chatModel(model)
                    .name("EmergencyVet")
                    .outputKey("answer")
                    .build();
            var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                    .chatModel(model)
                    .name("DogTrainer")
                    .outputKey("answer")
                    .build();
            var care = AgenticServices.agentBuilder(EverydayCare.class)
                    .chatModel(model)
                    .name("EverydayCare")
                    .outputKey("answer")
                    .build();
            Predicate<AgenticScope> isEmergency = s -> category(s.readState("category", "")).equals("emergency");
            Predicate<AgenticScope> isTraining = s -> category(s.readState("category", "")).equals("training");
            Predicate<AgenticScope> isEveryday = s -> category(s.readState("category", "")).equals("everyday");
            UntypedAgent routed = AgenticServices.conditionalBuilder()
                    .subAgents(isEmergency, vet)
                    .subAgents(isTraining, trainer)
                    .subAgents(isEveryday, care)
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, routed)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("worry", input));
            // Each desk ends by saying whether it could answer. That word is what the custom
            // planner's ladder branches on nine demos later; here it is protocol, not prose.
            return String.valueOf(r.result()).replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
        };
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                // The beat this demo plays in the running narration.
                "One of them was a whole bar of dark chocolate. Who do you ring?",
                // What this demo inherits from the ones before it.
                "Introduces the three desks that demos 7, 8, 15 and 16 all reuse.",
                "A router classifies the input and dispatches to the right specialist. Worth it "
                        + "when mis-routing is expensive: everyone in this room knows a dog that "
                        + "has eaten chocolate needs a vet and not a training tip, so everyone "
                        + "can see whether the classifier got it right.",
                "Only as good as the classifier, and unseen inputs fall through the cracks — so "
                        + "choose which way it falls. The fallback here is the vet, because that "
                        + "is the mistake you can live with.",
                topo,
                "he's just eaten a whole bar of dark chocolate off the coffee table, the wrapper "
                        + "is on the floor",
                runner);
    }
}
