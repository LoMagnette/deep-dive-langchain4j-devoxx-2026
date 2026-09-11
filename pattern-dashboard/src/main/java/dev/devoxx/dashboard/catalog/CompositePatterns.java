package dev.devoxx.dashboard.catalog;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static dev.devoxx.dashboard.support.Parsing.score;
import static dev.devoxx.dashboard.support.Wiring.agent;
import static dev.devoxx.dashboard.support.Wiring.result;
import static dev.devoxx.dashboard.support.Wiring.str;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.agents.composite.AngleScout;
import dev.devoxx.dashboard.agents.composite.CouncilBriefer;
import dev.devoxx.dashboard.agents.composite.CouncilNote;
import dev.devoxx.dashboard.agents.composite.HouseholdVerdict;
import dev.devoxx.dashboard.agents.composite.MealPlanner;
import dev.devoxx.dashboard.agents.composite.NoteTightener;
import dev.devoxx.dashboard.agents.composite.SecondDogAgainst;
import dev.devoxx.dashboard.agents.composite.SecondDogFor;
import dev.devoxx.dashboard.agents.composite.SitterNoteMerger;
import dev.devoxx.dashboard.agents.composite.WalkPlanner;
import dev.devoxx.dashboard.agents.workflow.DogTrainer;
import dev.devoxx.dashboard.agents.workflow.EmergencyVet;
import dev.devoxx.dashboard.agents.workflow.EverydayCare;
import dev.devoxx.dashboard.agents.workflow.FridgeRuleCheck;
import dev.devoxx.dashboard.agents.workflow.WorryRouter;
import dev.devoxx.dashboard.agents.zoo.AskZaoHimself;
import dev.devoxx.dashboard.agents.zoo.MoneyAndVet;
import dev.devoxx.dashboard.agents.zoo.SpaceAndTime;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;

/**
 * Systems rather than patterns: the payoff for the talk's arc. Each one nests the builders
 * above — every composite is itself an {@code UntypedAgent}, so a sequence can hold a loop
 * that holds a conditional.
 */
final class CompositePatterns {

    private CompositePatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(sitterNote(), secondDogCouncil());
    }

    // 14 — the capstone: four patterns composed into the note on the fridge door
    private static PatternDef sitterNote() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "the weekend", "input", 0),
                        node("router", "WorryRouter", "router", 1),
                        node("vet", "EmergencyVet", "agent", 2),
                        node("trainer", "DogTrainer", "agent", 2),
                        node("care", "EverydayCare", "agent", 2),
                        node("meals", "MealPlanner", "agent", 2),
                        node("walks", "WalkPlanner", "agent", 2),
                        node("merge", "SitterNoteMerger", "join", 3),
                        node("tighten", "NoteTightener", "agent", 4),
                        node("check", "FridgeRuleCheck", "agent", 4)),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday"),
                        edge("in", "meals", "in parallel"),
                        edge("in", "walks"),
                        edge("vet", "merge"), edge("trainer", "merge"),
                        edge("care", "merge", "answer"),
                        edge("meals", "merge"), edge("walks", "merge"),
                        edge("merge", "tighten", "note"),
                        edge("tighten", "check"),
                        edge("check", "tighten", "score < 0.8")));

        Runner runner = (model, input, listener) -> {
            // 1. Conditional routing — one LLM judgement decides who answers the worry.
            var router = agent(WorryRouter.class, model, "WorryRouter", "category");
            var vet = agent(EmergencyVet.class, model, "EmergencyVet", "answer");
            var trainer = agent(DogTrainer.class, model, "DogTrainer", "answer");
            var care = agent(EverydayCare.class, model, "EverydayCare", "answer");
            UntypedAgent triage = AgenticServices.conditionalBuilder()
                    .subAgents(s -> category(s).equals("emergency"), vet)
                    .subAgents(s -> category(s).equals("training"), trainer)
                    .subAgents(s -> category(s).equals("everyday"), care)
                    .build();

            // 2. Parallel — meals and walks do not need each other, so fan them out.
            var meals = agent(MealPlanner.class, model, "MealPlanner", "meals");
            var walks = agent(WalkPlanner.class, model, "WalkPlanner", "walks");
            UntypedAgent plan = AgenticServices.parallelBuilder()
                    .subAgents(meals, walks)
                    .build();

            // 3. Loop — refine the note until the four fridge-door rules hold, never forever.
            //    FridgeRuleCheck is the same agent the standalone loop demo uses: a composite
            //    reuses the parts, it does not re-implement them.
            var tighten = agent(NoteTightener.class, model, "NoteTightener", "note");
            var check = agent(FridgeRuleCheck.class, model, "FridgeRuleCheck", "score");
            UntypedAgent refine = AgenticServices.loopBuilder()
                    .subAgents(tighten, check)
                    .maxIterations(3)
                    .exitCondition(s -> score(s) >= 0.8)
                    .testExitAtLoopEnd(true)
                    .build();

            // 4. Sequence — the spine that holds the three composites plus the merge step.
            var merge = agent(SitterNoteMerger.class, model, "SitterNoteMerger", "note");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, triage, plan, merge, refine)
                    .outputKey("note")
                    .listener(listener)
                    .build();
            // The same text under two keys, and not by accident: the router and the three
            // specialists ask "what is the worry", the two planners ask "what is the stay".
            // Reusing an agent means accepting the key IT already declared — this one line is
            // the seam the caveat is about, and getting it wrong is a MissingArgumentException
            // pointing at a step that looks unrelated.
            var r = app.invokeWithAgenticScope(Map.of("worry", input, "stay", input));
            return result(r, "note");
        };

        return new PatternDef("sitterNote", "Sitter Note (composite)", "composite",
                "A real system, not a pattern: the owner's worry is routed to the right person, a "
                        + "parallel step plans the meals and the walks, a sequence merges all "
                        + "three into one note for the fridge door, and a loop tightens it until "
                        + "it passes the same four rules as the loop demo. Deterministic "
                        + "scaffolding with LLM judgement at exactly three points.",
                // caveat: the interesting failures in composites are at the seams, not inside them.
                "Composites fail at the seams: every step depends on a key an earlier one wrote, "
                        + "so one agent answering off-format breaks a step that looks unrelated.",
                topo,
                "we're away Friday to Sunday and my sister is having Zao. He's on two scoops "
                        + "morning and evening, he pulls like a train on the lead, and it's New "
                        + "Year so there will be fireworks both nights.",
                runner);
    }

    // 15 — the second capstone: two zoo patterns carried by simple plumbing
    private static PatternDef secondDogCouncil() {
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
            var scout = agent(AngleScout.class, model, "AngleScout", "finding");
            UntypedAgent survey = AgenticServices.parallelMapperBuilder()
                    .subAgents(scout)
                    .itemsProvider("angles")
                    .outputKey("findings")
                    .build();

            // 2. One plain agent (simple) turns the evidence into something debatable.
            var briefer = agent(CouncilBriefer.class, model, "CouncilBriefer", "motion");

            // 3. Debate (advanced) — the two sides argue, the chair rules.
            var forIt = agent(SecondDogFor.class, model, "SecondDogFor", null);
            var against = agent(SecondDogAgainst.class, model, "SecondDogAgainst", null);
            var chair = agent(HouseholdVerdict.class, model, "HouseholdVerdict", "verdict");
            UntypedAgent debate = AgenticServices.plannerBuilder()
                    .subAgents(forIt, against, chair)    // judge LAST
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .build();

            // 4. Glue (simple): the assessors vote on a 'household', the debate wrote a
            //    'verdict'. This one line is the whole lesson of this composite — see the caveat.
            var note = agent(CouncilNote.class, model, "CouncilNote", "household");

            // 5. Voting (advanced) — the same three assessors the voting demo used, now
            //    ratifying a debated motion instead of voting cold.
            var space = agent(SpaceAndTime.class, model, "SpaceAndTime", null);
            var money = agent(MoneyAndVet.class, model, "MoneyAndVet", null);
            var zao = agent(AskZaoHimself.class, model, "AskZaoHimself", null);
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
                return result(r, "verdict");
            }
            return "**The chair's ruling** — " + str(scope, "verdict")
                    + "\n\n**Put to the assessors as** — " + str(scope, "household")
                    + "\n\n**Ratified:** " + str(scope, "ratified");
        };

        return new PatternDef("secondDogCouncil", "Second Dog Council (composite)", "composite",
                "Settles the question the voting demo only took a snap poll on: a mapper reads "
                        + "three angles of the household at once, one agent turns them into a "
                        + "motion, a debate argues it to a ruling, and the same three assessors "
                        + "ratify it. Two zoo patterns carried by simple plumbing.",
                // caveat: the zoo patterns are the easy part; the adapters between them are not.
                "Most of this system is glue. Each zoo pattern expects its input under its own "
                        + "key — the debate writes 'verdict', the assessors read 'household' — so "
                        + "composing them is mostly writing the small steps in between.",
                topo,
                "should we get a second dog? " + ZooPatterns.HOUSEHOLD,
                runner);
    }
}
