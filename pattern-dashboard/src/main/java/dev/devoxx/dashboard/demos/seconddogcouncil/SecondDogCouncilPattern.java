package dev.devoxx.dashboard.demos.seconddogcouncil;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.demos.voting.VotingPattern.HOUSEHOLD;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.voting.AskZaoHimself;
import dev.devoxx.dashboard.demos.voting.MoneyAndVet;
import dev.devoxx.dashboard.demos.voting.SpaceAndTime;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;

/**
 * Wiring for the <b>second-dog council (composite)</b> demo — the second capstone: two zoo patterns carried by simple plumbing.
 */
public final class SecondDogCouncilPattern {

    private SecondDogCouncilPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "question", "input", 0),
                        node("scout", "AngleScout (per angle)", "agent", 1),
                        node("briefer", "CouncilBriefer", "join", 2),
                        node("for", "SecondDogFor", "agent", 3),
                        node("against", "SecondDogAgainst", "agent", 3),
                        node("chair", "HouseholdVerdict", "judge", 4),
                        node("note", "CouncilNote", "join", 5),
                        node("space", "SpaceAndTime", "agent", 6),
                        node("money", "MoneyAndVet", "agent", 6),
                        node("zao", "AskZaoHimself", "agent", 6),
                        node("tally", "majority()", "join", 7)),
                List.of(edge("in", "scout", "3 angles"),
                        edge("scout", "briefer", "findings"),
                        edge("briefer", "for", "motion"), edge("briefer", "against"),
                        edge("for", "against", "rebut"), edge("against", "for", "rebut"),
                        edge("for", "chair"), edge("against", "chair"),
                        edge("chair", "note", "verdict"),
                        edge("note", "space"), edge("note", "money"), edge("note", "zao"),
                        edge("space", "tally"), edge("money", "tally"), edge("zao", "tally")));

        Runner runner = (model, input, listener) -> {
            // 1. Parallel mapper (simple) — one scout over three angles of the household at once.
            var scout = AgenticServices.agentBuilder(AngleScout.class)
                    .chatModel(model)
                    .name("AngleScout")
                    .outputKey("finding")
                    .build();
            UntypedAgent survey = AgenticServices.parallelMapperBuilder()
                    .subAgents(scout)
                    .itemsProvider("angles")
                    .outputKey("findings")
                    .build();

            // 2. One plain agent (simple) turns the evidence into something debatable.
            var briefer = AgenticServices.agentBuilder(CouncilBriefer.class)
                    .chatModel(model)
                    .name("CouncilBriefer")
                    .outputKey("motion")
                    .build();

            // 3. Debate (advanced) — the two sides argue, the chair rules.
            var forIt = AgenticServices.agentBuilder(SecondDogFor.class)
                    .chatModel(model)
                    .name("SecondDogFor")
                    .build();
            var against = AgenticServices.agentBuilder(SecondDogAgainst.class)
                    .chatModel(model)
                    .name("SecondDogAgainst")
                    .build();
            var chair = AgenticServices.agentBuilder(HouseholdVerdict.class)
                    .chatModel(model)
                    .name("HouseholdVerdict")
                    .outputKey("verdict")
                    .build();
            UntypedAgent debate = AgenticServices.plannerBuilder()
                    .subAgents(forIt, against, chair)    // judge LAST
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .build();

            // 4. Glue (simple): the assessors vote on a 'household', the debate wrote a
            //    'verdict'. This one line is the whole lesson of this composite — see the caveat.
            var note = AgenticServices.agentBuilder(CouncilNote.class)
                    .chatModel(model)
                    .name("CouncilNote")
                    .outputKey("household")
                    .build();

            // 5. Voting (advanced) — the same three assessors the voting demo used, now
            //    ratifying a debated motion instead of voting cold.
            var space = AgenticServices.agentBuilder(SpaceAndTime.class)
                    .chatModel(model)
                    .name("SpaceAndTime")
                    .build();
            var money = AgenticServices.agentBuilder(MoneyAndVet.class)
                    .chatModel(model)
                    .name("MoneyAndVet")
                    .build();
            var zao = AgenticServices.agentBuilder(AskZaoHimself.class)
                    .chatModel(model)
                    .name("AskZaoHimself")
                    .build();
            UntypedAgent ratify = AgenticServices.plannerBuilder()
                    .subAgents(space, money, zao)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("ratified")
                    .build();

            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(survey, briefer, debate, note, ratify)
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            // The angles are derived here rather than by an agent: the mapper needs a real
            // collection in scope before anything has run.
            var r = app.invokeWithAgenticScope(Map.of(
                    "question", input,
                    "angles", List.of("the space and the hours alone — " + input,
                            "the money over ten years — " + input,
                            "what Zao would say about it — " + input)));
            // The last stage is the vote, so the result has to show it: returning only the
            // debate's verdict would leave the ratification invisible and the final third of the
            // diagram looking decorative.
            var scope = r.agenticScope();
            if (scope == null) {
                return String.valueOf(r.result());
            }
            return "**The chair's ruling** — " + scope.readState("verdict", "")
                    + "\n\n**Put to the assessors as** — " + scope.readState("household", "")
                    + "\n\n**Ratified:** " + scope.readState("ratified", "");
        };

        return new PatternDef("secondDogCouncil", "Second Dog Council (composite)", "composite",
                // The beat this demo plays in the running narration.
                "And the second dog, finally put properly to the household.",
                "Settles the question the voting demo only took a snap poll on: a mapper reads "
                        + "three angles of the household at once, one agent turns them into a "
                        + "motion, a debate argues it to a ruling, and the same three assessors "
                        + "ratify it. Two zoo patterns carried by simple plumbing.",
                // caveat: the zoo patterns are the easy part; the adapters between them are not.
                "Most of this system is glue. Each zoo pattern expects its input under its own "
                        + "key — the debate writes 'verdict', the assessors read 'household' — so "
                        + "composing them is mostly writing the small steps in between.",
                topo,
                "should we get a second dog? " + HOUSEHOLD,
                runner);
    }
}
