package dev.devoxx.dashboard.demos._06_conditional;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
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
        var rescue = AgenticServices.agentBuilder(RescueDog.class)
                .chatModel(model)
                .name("RescueDog")
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

        RoutedDesk routed = AgenticServices.conditionalBuilder(RoutedDesk.class)
                .name("Conditional")
                .subAgents(s -> "emergency".equals(category(s.readState(Category.class))), rescue)
                .subAgents(s -> "training".equals(category(s.readState(Category.class))), trainer)
                .subAgents(s -> "everyday".equals(category(s.readState(Category.class))), care)
                .build();

        TriagePipeline app = AgenticServices.sequenceBuilder(TriagePipeline.class)
                .name("Sequential")
                .subAgents(router, routed)
                .outputKey(Answer.class)
                .listener(listener)
                .build();

        return app.answer(input).replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "worry", "input"),
                        node("router", "WorryRouter", "router").withSub("Corgi · herds worries"),
                        node("vet", "RescueDog", "agent").withSub("St Bernard · rescue"),
                        node("trainer", "DogTrainer", "agent").withSub("Border Collie · trains"),
                        node("care", "EverydayCare", "agent").withSub("Golden · the everyday")),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday")));
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "Meanwhile the Dachshund went under the fence after it. Only his back half came "
                        + "back. The back half is wagging.",
                "Introduces the three desks that demos 7, 9, 16 and 17 all reuse.",
                "A router classifies the input and dispatches to the right specialist. Worth it "
                        + "when mis-routing is expensive: everyone in this room knows a dog stuck "
                        + "under a fence needs the rescue dog and not a training tip, so everyone "
                        + "can see whether the classifier got it right.",
                "Only as good as the classifier, and unseen inputs fall through the cracks — so "
                        + "choose which way it falls. The fallback here is the rescue dog, because that "
                        + "is the mistake you can live with.",
                topo,
                "the Dachshund went under the fence after the squirrel and is stuck halfway — his "
                        + "front half is in next door's garden, his back half is in ours, and the "
                        + "back half is wagging",
                ConditionalPattern::run);
    }
}
