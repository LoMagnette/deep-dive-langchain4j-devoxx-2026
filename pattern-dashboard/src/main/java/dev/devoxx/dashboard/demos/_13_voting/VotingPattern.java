package dev.devoxx.dashboard.demos._13_voting;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos._07_humanapproval.Keys.Decision;
import dev.devoxx.dashboard.demos._13_voting.Keys.Household;
import dev.devoxx.dashboard.demos._13_voting.Keys.MoneyVote;
import dev.devoxx.dashboard.demos._13_voting.Keys.SpaceVote;
import dev.devoxx.dashboard.demos._13_voting.Keys.ZaoVote;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
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
     * The household the three criteria judge. Shared with the council demo, which puts the same
     * question through a whole debate before these same three assessors ratify the answer.
     */
    public static final String HOUSEHOLD =
            "two-bedroom flat, no garden, both of us out from eight until six, we can afford a "
                    + "second one comfortably. Zao is four and he stiffens up and growls when "
                    + "another dog comes at him in the park.";

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // Three DIFFERENT criteria, or the tally is decoration. Each voter writes its own key
        // too: the strategy does not need it, but a split is invisible without it.
        var space = AgenticServices.agentBuilder(SpaceAndTime.class)
                .chatModel(model)
                .name("SpaceAndTime")
                .outputKey(SpaceVote.class)
                .build();
        var money = AgenticServices.agentBuilder(MoneyAndVet.class)
                .chatModel(model)
                .name("MoneyAndVet")
                .outputKey(MoneyVote.class)
                .build();
        var zao = AgenticServices.agentBuilder(AskZaoHimself.class)
                .chatModel(model)
                .name("AskZaoHimself")
                .outputKey(ZaoVote.class)
                .build();
        UntypedAgent app = AgenticServices.plannerBuilder()
                .subAgents(space, money, zao)
                .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                .outputKey(Decision.class)
                .listener(listener)
                .build();
        var r = app.invokeWithAgenticScope(Map.of(new Household().name(), input));
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String decision = requireNonNullElse(scope.readState(Decision.class), "");
        String spaceVote = requireNonNullElse(scope.readState(SpaceVote.class), "");
        String moneyVote = requireNonNullElse(scope.readState(MoneyVote.class), "");
        String zaoVote = requireNonNullElse(scope.readState(ZaoVote.class), "");
        return "**Majority: " + decision + "**\n\n"
                + "- Space and hours alone: " + spaceVote + "\n"
                + "- Money: " + moneyVote + "\n"
                + "- Zao himself: " + zaoVote;
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "household", "input"),
                        node("space", "SpaceAndTime", "agent"),
                        node("money", "MoneyAndVet", "agent"),
                        node("zao", "AskZaoHimself", "agent"),
                        // Without the tally this is just a fan-out; the tally IS the pattern.
                        node("vote", "majority()", "join")),
                List.of(edge("in", "space"), edge("in", "money"), edge("in", "zao"),
                        edge("space", "vote", "YES / LATER"),
                        edge("money", "vote"), edge("zao", "vote")));
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "The question that will not go away: would he be happier with another dog? "
                        + "Everyone in the house already has an answer.",
                "Introduces the three assessors the council reuses in demo 18.",
                "Several agents answer independently; a strategy aggregates (majority, average, "
                        + "highest). Worth the tokens when one judgement is not trustworthy "
                        + "enough to act on — and this household is a genuine split, because the "
                        + "money is fine and everything else is not.",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity of criteria is what "
                        + "buys robustness, not running the same prompt three times. Note the "
                        + "price: each voter answers in ONE word, because a strategy can only "
                        + "tally answers that can be equal.",
                topo, HOUSEHOLD, VotingPattern::run);
    }
}
