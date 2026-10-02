package dev.devoxx.dashboard.demos._19_modelrouting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Response;
import dev.devoxx.dashboard.demos._06_conditional.ZaoClassifies;
import dev.devoxx.dashboard.run.ModelTiers;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for <b>Mission 19</b> — one agent, two models, and the call decides which.
 */
public final class ModelRoutingPattern {

    private ModelRoutingPattern() {
    }

    /** Where being wrong is expensive, so the strong model answers. */
    private static final Set<String> SERIOUS = Set.of("hurt", "urgent");

    static String run(ChatModel model, String input, StreamingListener listener) {
        ModelTiers tiers = listener.tiers(model);
        // Which tier actually answered, recorded where the choice is made. Nothing in the scope
        // records it — the framework has no reason to — and a demo that cannot say which model
        // ran has not demonstrated anything.
        AtomicReference<String> picked = new AtomicReference<>("none");

        // Classifying is the cheap job by definition: one word of output.
        var zao = AgenticServices.agentBuilder(ZaoClassifies.class)
                .chatModel(tiers.cheap())
                .name("Zao")
                .outputKey(Category.class)
                .build();

        // THE line. chatModel takes a Function<AgenticScope, ChatModel>, so the model is
        // resolved when Doc is invoked — by which time Zao has written the category.
        var doc = AgenticServices.agentBuilder(DocOnNights.class)
                .chatModel(scope -> {
                    boolean serious = SERIOUS.contains(category(scope.readState(Category.class)));
                    picked.set(serious ? tiers.strongName() : tiers.cheapName());
                    return serious ? tiers.strong() : tiers.cheap();
                })
                .name("Doc")
                .outputKey(Response.class)
                .build();

        NightPhone app = AgenticServices.sequenceBuilder(NightPhone.class)
                .name("Sequential")
                .subAgents(zao, doc)
                .output(scope -> answerWithItsTier(scope, tiers, picked.get()))
                .listener(listener)
                .build();
        return app.answer(input);
    }

    /** Leads with the choice: the answer alone looks identical whichever model produced it. */
    private static String answerWithItsTier(AgenticScope scope, ModelTiers tiers, String picked) {
        return "**" + category(scope.readState(Category.class)) + " → " + picked + "**\n\n"
                + scope.readState(Response.class) + "\n\n---\n\n*" + tiers.note() + "*";
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // One box for Doc, not two: two boxes both labelled Doc would both light up on a run,
        // which would say both models answered. The choice lives on the sub-line, where it is true.
        Topology.Graph topo = graph("stages",
                List.of(node("in", "call", "input", 0),
                        node("zao", "Zao", "router", 1).withSub("always the cheap model").as("zao"),
                        node("doc", "Doc", "agent", 2).withSub("cheap · or strong").as("doc"),
                        node("out", "the answer", "join", 3).withSub("names the model")),
                List.of(edge("in", "zao"),
                        edge("zao", "doc", "category picks the model"),
                        edge("doc", "out")));
        return new PatternDef("modelRouting", "Dynamic Model Selection", "production",
                "The night phone. You do not wake the big brain for a lost umbrella, and you do "
                        + "not ask the small one about a swollen ankle.",
                "Mission 6's classifier, unchanged. The four Rangers are gone — here one answers "
                        + "everything and only the model behind him changes.",
                "`chatModel(...)` has an overload taking a `Function<AgenticScope, ChatModel>`, "
                        + "so the model is resolved **when the agent is invoked** rather than when "
                        + "it is built — by which time Zao has written `category`. One agent, one "
                        + "prompt: anything that differs between two runs came from the model. "
                        + "\"I have lost my umbrella\" settles on the cheap one; the ankle goes to "
                        + "the strong one.",
                "The classifier is now a **cost decision as well as a routing one**, so its "
                        + "failures are asymmetric: cheap-when-it-should-be-strong is the expensive "
                        + "mistake, and it is the silent one. Bias the fallback upward — here, "
                        + "anything Zao cannot place is treated as hurt.",
                topo,
                "Pup HQ night phone: Grandpa Biscuit slipped on the ice outside the bakery, and "
                        + "his ankle has swollen up like a melon.",
                ModelRoutingPattern::run);
    }
}
