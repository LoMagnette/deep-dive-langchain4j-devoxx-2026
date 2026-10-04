package dev.devoxx.dashboard.demos._19_modelrouting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;

import java.util.List;
import java.util.Set;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Category;
import dev.devoxx.dashboard.demos._06_conditional.Keys.Response;
import dev.devoxx.dashboard.run.CurrentRun;
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
        // The system's default model is the CHEAP one. Classifying is the cheap job by
        // definition, so Zao needs no wiring at all — and Doc's @ChatModelSupplier is the one
        // place in the mission that ever asks for more.
        return CurrentRun.with(listener, tiers, () ->
                AgenticServices.createAgenticSystem(NightPhone.class, tiers.cheap()).answer(input));
    }

    /** True for a category where being wrong is expensive — the one rule that picks the model. */
    static boolean serious(String category) {
        return SERIOUS.contains(category(category));
    }

    /**
     * Leads with the choice: the answer alone looks identical whichever model produced it. The
     * tier is a pure function of the category, so it is read back through the same
     * {@link #serious} that {@code DocOnNights.model} chose with — not guessed after the fact.
     */
    static String answerWithItsTier(AgenticScope scope, ModelTiers tiers) {
        String category = scope.readState(Category.class);
        String picked = serious(category) ? tiers.strongName() : tiers.cheapName();
        return "**" + category(category) + " → " + picked + "**\n\n"
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
                "Doc's `@ChatModelSupplier` takes a parameter — `@K(Category.class)` — so the "
                        + "model is resolved **when the agent is invoked** rather than when it is "
                        + "built — by which time Zao has written `category`. One agent, one "
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
                ModelRoutingPattern::run)
                .gist("A cheap or a strong model, chosen per call from the scope.");
    }
}
