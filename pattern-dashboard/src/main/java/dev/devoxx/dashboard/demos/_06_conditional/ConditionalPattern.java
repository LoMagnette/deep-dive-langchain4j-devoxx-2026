package dev.devoxx.dashboard.demos._06_conditional;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>conditional routing</b> demo — where sending it to the wrong person is the disaster.
 */
public final class ConditionalPattern {

    private ConditionalPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        var router = AgenticServices.agentBuilder(WorryRouter.class)
                .chatModel(model)
                .name("WorryRouter")
                .outputKey(Category.class)
                .build();
        var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                .chatModel(model)
                .name("EmergencyVet")
                .outputKey(Answer.class)
                .build();
        var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                .chatModel(model)
                .name("DogTrainer")
                .outputKey(Answer.class)
                .build();
        var care = AgenticServices.agentBuilder(EverydayCare.class)
                .chatModel(model)
                .name("EverydayCare")
                .outputKey(Answer.class)
                .build();
        Predicate<AgenticScope> isEmergency =
                s -> category(s.readState(Category.class)).equals("emergency");
        Predicate<AgenticScope> isTraining =
                s -> category(s.readState(Category.class)).equals("training");
        Predicate<AgenticScope> isEveryday =
                s -> category(s.readState(Category.class)).equals("everyday");
        UntypedAgent routed = AgenticServices.conditionalBuilder()
                .subAgents(isEmergency, vet)
                .subAgents(isTraining, trainer)
                .subAgents(isEveryday, care)
                .build();
        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(router, routed)
                .outputKey(Answer.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Worry().name(), input));
        // Each desk ends by saying whether it could answer. That word is what the custom
        // planner's ladder branches on nine demos later; here it is protocol, not prose.
        return String.valueOf(r.result()).replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
    }

    /** How the page draws it, and what the catalogue shows. */
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
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "One of them was a whole bar of dark chocolate. This is not a training "
                        + "question, and he is not sorry.",
                "Introduces the three desks that demos 7, 9, 16 and 17 all reuse.",
                "A router classifies the input and dispatches to the right specialist. Worth it "
                        + "when mis-routing is expensive: everyone in this room knows a dog that "
                        + "has eaten chocolate needs a vet and not a training tip, so everyone "
                        + "can see whether the classifier got it right.",
                "Only as good as the classifier, and unseen inputs fall through the cracks — so "
                        + "choose which way it falls. The fallback here is the vet, because that "
                        + "is the mistake you can live with.",
                topo,
                "he's eaten a whole bar of dark chocolate off the coffee table — 85%, the good "
                        + "stuff. The wrapper is on the floor and he is wagging",
                ConditionalPattern::run);
    }
}
