package dev.devoxx.dashboard.demos.voting;

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
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;

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
        Runner runner = (model, input, listener) -> {
            // Three DIFFERENT criteria over the same household — space and hours, money, and
            // what the dog you already have would say. Three copies of one prompt (what this
            // demo used to be) always agree, so the tally was decoration. Here money says yes
            // while the other two say later, which is the only situation where a majority means
            // anything.
            //
            // Each voter also writes its own key. The strategy does not need them — it tallies
            // what the agents returned — but the result pane does: one word on its own hides the
            // only interesting thing, which is whether they split.
            var space = AgenticServices.agentBuilder(SpaceAndTime.class)
                    .chatModel(model)
                    .name("SpaceAndTime")
                    .outputKey("spaceVote")
                    .build();
            var money = AgenticServices.agentBuilder(MoneyAndVet.class)
                    .chatModel(model)
                    .name("MoneyAndVet")
                    .outputKey("moneyVote")
                    .build();
            var zao = AgenticServices.agentBuilder(AskZaoHimself.class)
                    .chatModel(model)
                    .name("AskZaoHimself")
                    .outputKey("zaoVote")
                    .build();
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(space, money, zao)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("decision")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("household", input));
            var scope = r.agenticScope();
            if (scope == null) {
                return String.valueOf(r.result());
            }
            return "**Majority: " + scope.readState("decision", "") + "**\n\n"
                    + "- Space and hours alone: " + scope.readState("spaceVote", "") + "\n"
                    + "- Money: " + scope.readState("moneyVote", "") + "\n"
                    + "- Zao himself: " + scope.readState("zaoVote", "");
        };
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                // The beat this demo plays in the running narration.
                "The question that will not go away: would he be happier with another dog?",
                // What this demo inherits from the ones before it.
                "Introduces the three assessors the council reuses in demo 17.",
                "Several agents answer independently; a strategy aggregates (majority, average, "
                        + "highest). Worth the tokens when one judgement is not trustworthy "
                        + "enough to act on — and this household is a genuine split, because the "
                        + "money is fine and everything else is not.",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity of criteria is what "
                        + "buys robustness, not running the same prompt three times. Note the "
                        + "price: each voter answers in ONE word, because a strategy can only "
                        + "tally answers that can be equal.",
                topo, HOUSEHOLD, runner);
    }
}
