package dev.devoxx.dashboard.demos.seconddogcouncil;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.demos.voting.VotingPattern.HOUSEHOLD;
import static java.util.Objects.requireNonNullElse;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.debate.Keys.Motion;
import dev.devoxx.dashboard.demos.debate.Keys.Verdict;
import dev.devoxx.dashboard.demos.p2p.Keys.Question;
import dev.devoxx.dashboard.demos.seconddogcouncil.Keys.Angles;
import dev.devoxx.dashboard.demos.seconddogcouncil.Keys.Finding;
import dev.devoxx.dashboard.demos.seconddogcouncil.Keys.Findings;
import dev.devoxx.dashboard.demos.seconddogcouncil.Keys.Ratified;
import dev.devoxx.dashboard.demos.voting.AskZaoHimself;
import dev.devoxx.dashboard.demos.voting.Keys.Household;
import dev.devoxx.dashboard.demos.voting.MoneyAndVet;
import dev.devoxx.dashboard.demos.voting.SpaceAndTime;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>second-dog council (composite)</b> demo — the second capstone: two zoo patterns carried by simple plumbing.
 */
public final class SecondDogCouncilPattern {

    private SecondDogCouncilPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // 1. Parallel mapper (simple) — one scout over three angles of the household at once.
        var scout = AgenticServices.agentBuilder(AngleScout.class)
                .chatModel(model)
                .name("AngleScout")
                .outputKey(Finding.class)
                .build();
        UntypedAgent survey = AgenticServices.parallelMapperBuilder()
                .subAgents(scout)
                .itemsProvider(new Angles().name())
                .outputKey(Findings.class)
                .build();

        // 2. One plain agent (simple) turns the evidence into something debatable.
        var briefer = AgenticServices.agentBuilder(CouncilBriefer.class)
                .chatModel(model)
                .name("CouncilBriefer")
                .outputKey(Motion.class)
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
                .outputKey(Verdict.class)
                .build();
        UntypedAgent debate = AgenticServices.plannerBuilder()
                .subAgents(forIt, against, chair)    // judge LAST
                .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                .outputKey(Verdict.class)
                .build();

        // 4. Glue (simple): the assessors vote on a 'household', the debate wrote a
        //    'verdict'. This one line is the whole lesson of this composite — see the caveat.
        var note = AgenticServices.agentBuilder(CouncilNote.class)
                .chatModel(model)
                .name("CouncilNote")
                .outputKey(Household.class)
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
                .outputKey(Ratified.class)
                .build();

        UntypedAgent app = AgenticServices.sequenceBuilder()
                .subAgents(survey, briefer, debate, note, ratify)
                .outputKey(Verdict.class)
                .listener(listener)
                .build();
        // The angles are derived here rather than by an agent: the mapper needs a real
        // collection in scope before anything has run.
        var r = app.invokeWithAgenticScope(Map.of(
                new Question().name(), input,
                new Angles().name(), List.of("the space and the hours alone — " + input,
                        "the money over ten years — " + input,
                        "what Zao would say about it — " + input)));
        // The last stage is the vote, so the result has to show it: returning only the
        // debate's verdict would leave the ratification invisible and the final third of the
        // diagram looking decorative.
        var scope = r.agenticScope();
        if (scope == null) {
            return String.valueOf(r.result());
        }
        String verdictText = requireNonNullElse(scope.readState(Verdict.class), "");
        String householdText = requireNonNullElse(scope.readState(Household.class), "");
        String ratifiedText = requireNonNullElse(scope.readState(Ratified.class), "");
        return "**The chair's ruling** — " + verdictText
                + "\n\n**Put to the assessors as** — " + householdText
                + "\n\n**Ratified:** " + ratifiedText;
    }

    /** How the page draws it, and what the catalogue shows. */
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

        return new PatternDef("secondDogCouncil", "Second Dog Council (composite)", "composite",
                "And the second dog, finally put properly to the household. Zao has not been "
                        + "asked. Zao is on the committee.",
                "Demo 13's three assessors, now ratifying a motion that has been debated "
                        + "rather than voting on it cold.",
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
                SecondDogCouncilPattern::run);
    }
}
