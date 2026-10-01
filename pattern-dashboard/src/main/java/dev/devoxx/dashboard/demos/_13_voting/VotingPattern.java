package dev.devoxx.dashboard.demos._13_voting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Decision;
import dev.devoxx.dashboard.demos._13_voting.Keys.FoodVote;
import dev.devoxx.dashboard.demos._13_voting.Keys.SofaVote;
import dev.devoxx.dashboard.demos._13_voting.Keys.ZaoVote;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>voting</b> demo — three criteria that can genuinely disagree.
 */
public final class VotingPattern {

    private VotingPattern() {
    }

    /**
     * The pack the three criteria judge. Shared with the council demo, which puts the same
     * question through a whole debate before these same three assessors ratify the answer.
     */
    public static final String HOUSEHOLD =
            "an eight-week-old puppy, if the pack says yes, on Saturday. The food is fine — the "
                    + "human has just bought a twenty-kilo bag, which is the only part of this "
                    + "that is easy. The sofa already holds a Greyhound, a Labrador and the "
                    + "cushion nobody is allowed on. Zao is four, and when another dog comes at "
                    + "him in the park he goes stiff and makes a noise nobody has a word for.";

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // Three DIFFERENT criteria, or the tally is decoration. Each voter writes its own key
        // too: the strategy does not need it, but a split is invisible without it.
        var space = AgenticServices.agentBuilder(SofaSpace.class)
                .chatModel(model)
                .name("SofaSpace")
                .outputKey(SofaVote.class)
                .build();
        var money = AgenticServices.agentBuilder(FoodBudget.class)
                .chatModel(model)
                .name("FoodBudget")
                .outputKey(FoodVote.class)
                .build();
        var zao = AgenticServices.agentBuilder(AskZaoHimself.class)
                .chatModel(model)
                .name("AskZaoHimself")
                .outputKey(ZaoVote.class)
                .build();
        Ballot app = AgenticServices.plannerBuilder(Ballot.class)
                .subAgents(space, money, zao)
                .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                .outputKey(Decision.class)
                .listener(listener)
                .build();
        var r = app.invoke(input);
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String decision = requireNonNullElse(scope.readState(Decision.class), "");
        String sofaVote = requireNonNullElse(scope.readState(SofaVote.class), "");
        String foodVote = requireNonNullElse(scope.readState(FoodVote.class), "");
        String zaoVote = requireNonNullElse(scope.readState(ZaoVote.class), "");
        return "**Majority: " + decision + "**\n\n"
                + "- The Greyhound, on space: " + sofaVote + "\n"
                + "- The Labrador, on food: " + foodVote + "\n"
                + "- Zao himself: " + zaoVote;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "the pack", "input"),
                        node("space", "SofaSpace", "agent").withSub("Greyhound · space"),
                        node("money", "FoodBudget", "agent").withSub("Labrador · food"),
                        node("zao", "AskZaoHimself", "agent").withSub("Zao · himself"),
                        // Without the tally this is just a fan-out; the tally IS the pattern.
                        // The sub-line is the thing people get wrong about ensembles: a
                        // strategy can only tally answers that can be EQUAL, which is why
                        // every voter here is asked for one word.
                        node("vote", "majority()", "join").withSub("tallies one-word votes")),
                List.of(edge("in", "space"), edge("in", "money"), edge("in", "zao"),
                        edge("space", "vote", "YES / LATER"),
                        edge("money", "vote"), edge("zao", "vote")));
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "The question that will not go away: should the pack take in a puppy? Every "
                        + "dog in the house already has an answer.",
                "Introduces the three assessors the council reuses in demo 18.",
                "Several agents answer independently; a strategy aggregates (majority, average, "
                        + "highest). Worth the tokens when one judgement is not trustworthy "
                        + "enough to act on — and this pack is a genuine split, because the "
                        + "food is fine and everything else is not.",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity of criteria is what "
                        + "buys robustness, not running the same prompt three times. Note the "
                        + "price: each voter answers in ONE word, because a strategy can only "
                        + "tally answers that can be equal.",
                topo, HOUSEHOLD, VotingPattern::run);
    }
}
