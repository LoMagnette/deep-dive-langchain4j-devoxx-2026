package dev.devoxx.dashboard.demos._19_modelrouting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Answer;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Worry;
import dev.devoxx.dashboard.demos._06_conditional.WorryRouter;
import dev.devoxx.dashboard.run.ModelTiers;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>dynamic model selection</b> demo — one agent, one prompt, two brains.
 */
public final class ModelRoutingPattern {

    private ModelRoutingPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        ModelTiers tiers = listener.tiers(model);
        // Which tier actually answered, recorded where the choice is made. Nothing in the scope
        // records it — the framework has no reason to — and a demo that cannot say which model
        // ran has not demonstrated anything.
        AtomicReference<String> picked = new AtomicReference<>("none");

        // Classifying is the cheap job by definition: it is one word of output, and getting it
        // wrong is caught by the next step rather than reaching the owner.
        var router = AgenticServices.agentBuilder(WorryRouter.class)
                .chatModel(tiers.cheap())
                .name("WorryRouter")
                .outputKey(Category.class)
                .build();

        // THE line. chatModel takes a Function<AgenticScope, ChatModel>, so the model is
        // resolved at invocation — by which time the router has written the category.
        var desk = AgenticServices.agentBuilder(DutyDesk.class)
                .chatModel(scope -> {
                    boolean serious = "emergency".equals(category(scope.readState(Category.class)));
                    picked.set(serious ? tiers.strongName() : tiers.cheapName());
                    return serious ? tiers.strong() : tiers.cheap();
                })
                .name("DutyDesk")
                .outputKey(Answer.class)
                .build();

        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(router, desk)
                .output(scope -> answerWithItsTier(scope, tiers, picked.get()))
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Worry().name(), input));
        return String.valueOf(r.result());
    }

    /**
     * Leads with the choice, because the answer alone is the one thing that does <i>not</i>
     * demonstrate this pattern — it looks identical whichever model produced it.
     */
    private static String answerWithItsTier(AgenticScope scope, ModelTiers tiers, String picked) {
        String kind = category(scope.readState(Category.class));
        return "**" + kind + " → " + picked + "**\n\n"
                + String.valueOf(scope.readState(Answer.class))
                        .replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "")
                + "\n\n---\n\n*" + tiers.note() + "*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // One box for the desk, not two. Two boxes both labelled DutyDesk would both light up on
        // a run — the diagram marks nodes by agent name — which would say both models answered,
        // and only one ever does. So the choice lives on the sub-line, where it is true.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "worry", "input", 0),
                        node("router", "WorryRouter", "router", 1)
                                .withSub("always the cheap tier"),
                        node("desk", "DutyDesk", "agent", 2)
                                .withSub("cheap · or strong"),
                        node("out", "the answer", "join", 3).withSub("names the tier")),
                List.of(edge("in", "router"),
                        edge("router", "desk", "category picks the model"),
                        edge("desk", "out")));
        return new PatternDef("modelRouting", "Dynamic Model Selection", "production",
                "You do not ring the emergency vet to ask which kibble to buy, and you do not "
                        + "ask the pet shop about a face swelling shut.",
                "Demo 6's WorryRouter, unchanged. The three desks are gone — here one "
                        + "agent answers everything and only its model changes.",
                "`chatModel(...)` has an overload taking a `Function<AgenticScope, ChatModel>`, "
                        + "so the model is resolved **when the agent is invoked** rather than "
                        + "when it is built — by which time the router has written `category`. "
                        + "One agent, one prompt: the only variable is which model is behind it, "
                        + "so anything that differs between two runs came from the tier. Type "
                        + "\"which food should I buy?\" and it settles on the cheap one; the "
                        + "chocolate goes to the strong one.",
                "The classifier is now a **cost decision as well as a routing one**, so its "
                        + "failures are asymmetric: cheap-when-it-should-be-strong is the "
                        + "expensive mistake, and it is the silent one. Bias the fallback "
                        + "upward. Note also that nothing in the scope records which model "
                        + "answered — if you need that for a bill, you have to write it down "
                        + "yourself.",
                topo,
                "a wasp has stung him on the nose and it has swollen up like a tennis ball. He "
                        + "has gone to find the wasp",
                ModelRoutingPattern::run);
    }
}
