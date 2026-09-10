package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Parsing.category;
import static dev.devoxx.dashboard.Parsing.score;
import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.PatternDef.Runner;
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
        return List.of(kennelDesk(), packCouncil());
    }

    // 14 — the capstone: four patterns composed into one system
    private static PatternDef kennelDesk() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "request", "input", 0),
                        node("router", "KennelRouter", "router", 1),
                        node("behaviour", "BehaviourExpert", "agent", 2),
                        node("nutrition", "NutritionExpert", "agent", 2),
                        node("vet", "VetExpert", "agent", 2),
                        node("activity", "ActivityPlanner", "agent", 2),
                        node("meal", "MealPlanner", "agent", 2),
                        node("writer", "CarePlanWriter", "join", 3),
                        node("editor", "PlanEditor", "agent", 4),
                        node("critic", "PlanCritic", "agent", 4)),
                List.of(edge("in", "router"),
                        edge("router", "behaviour", "behaviour"),
                        edge("router", "nutrition", "nutrition"),
                        edge("router", "vet", "veterinary"),
                        edge("in", "activity", "in parallel"),
                        edge("in", "meal"),
                        edge("behaviour", "writer"), edge("nutrition", "writer"),
                        edge("vet", "writer", "answer"),
                        edge("activity", "writer"), edge("meal", "writer"),
                        edge("writer", "editor", "plan"),
                        edge("editor", "critic"),
                        edge("critic", "editor", "score < 0.8")));

        Runner runner = (model, input, listener) -> {
            // 1. Conditional routing — one LLM judgement decides which specialist answers.
            var router = agent(Agents.KennelRouter.class, model, "KennelRouter", "category");
            var behaviour = agent(Agents.BehaviourExpert.class, model, "BehaviourExpert", "answer");
            var nutrition = agent(Agents.NutritionExpert.class, model, "NutritionExpert", "answer");
            var vet = agent(Agents.VetExpert.class, model, "VetExpert", "answer");
            UntypedAgent triage = AgenticServices.conditionalBuilder()
                    .subAgents(s -> category(s).equals("behaviour"), behaviour)
                    .subAgents(s -> category(s).equals("nutrition"), nutrition)
                    .subAgents(s -> category(s).equals("veterinary"), vet)
                    .build();

            // 2. Parallel — the day's two halves are independent, so fan them out.
            var activity = agent(Agents.ActivityPlanner.class, model, "ActivityPlanner", "activity");
            var meal = agent(Agents.MealPlanner.class, model, "MealPlanner", "meal");
            UntypedAgent gather = AgenticServices.parallelBuilder()
                    .subAgents(activity, meal)
                    .build();

            // 3. Loop — refine the merged plan until the critic is satisfied, but never forever.
            var editor = agent(Agents.PlanEditor.class, model, "PlanEditor", "plan");
            var critic = agent(Agents.PlanCritic.class, model, "PlanCritic", "score");
            UntypedAgent refine = AgenticServices.loopBuilder()
                    .subAgents(editor, critic)
                    .maxIterations(3)
                    .exitCondition(s -> score(s) >= 0.8)
                    .testExitAtLoopEnd(true)
                    .build();

            // 4. Sequence — the spine that holds the three composites plus the merge step.
            var writer = agent(Agents.CarePlanWriter.class, model, "CarePlanWriter", "plan");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, triage, gather, writer, refine)
                    .outputKey("plan")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("request", input));
            return result(r, "plan");
        };

        return new PatternDef("kennelDesk", "Kennel Desk (composite)", "composite",
                "A real system, not a pattern: routing picks a specialist, a parallel step plans "
                        + "the day, a sequence merges both into a plan, and a loop refines it until "
                        + "a critic passes it.",
                // caveat: the interesting failures in composites are at the seams, not inside them.
                "Composites fail at the seams: every step depends on a key an earlier one wrote, so "
                        + "one agent answering off-format breaks a step that looks unrelated.",
                topo, "Zao has been limping after long walks and refuses his dinner", runner);
    }

    // 15 — the second capstone: two zoo patterns carried by simple plumbing
    private static PatternDef packCouncil() {
        Topology.Graph topo = graph("stages",
                List.of(node("in", "question", "input", 0),
                        node("scout", "PackScout (per angle)", "agent", 1),
                        node("briefer", "CouncilBriefer", "join", 2),
                        node("pro", "DogAdvocate", "agent", 3),
                        node("con", "HouseholdAdvocate", "agent", 3),
                        node("judge", "PackJudge", "judge", 4),
                        node("note", "CouncilNote", "join", 5),
                        node("a", "MoodSnifferA", "agent", 6),
                        node("b", "MoodSnifferB", "agent", 6),
                        node("c", "MoodSnifferC", "agent", 6),
                        node("tally", "majority()", "join", 7)),
                List.of(edge("in", "scout", "3 angles"),
                        edge("scout", "briefer", "findings"),
                        edge("briefer", "pro", "motion"), edge("briefer", "con"),
                        edge("pro", "con", "rebut"), edge("con", "pro", "rebut"),
                        edge("pro", "judge"), edge("con", "judge"),
                        edge("judge", "note", "verdict"),
                        edge("note", "a"), edge("note", "b"), edge("note", "c"),
                        edge("a", "tally"), edge("b", "tally"), edge("c", "tally")));

        Runner runner = (model, input, listener) -> {
            // 1. Parallel mapper (simple) — the same scout runs over three angles at once.
            var scout = agent(Agents.PackScout.class, model, "PackScout", "finding");
            UntypedAgent survey = AgenticServices.parallelMapperBuilder()
                    .subAgents(scout)
                    .itemsProvider("angles")
                    .outputKey("findings")
                    .build();

            // 2. One plain agent (simple) turns the evidence into something debatable.
            var briefer = agent(Agents.CouncilBriefer.class, model, "CouncilBriefer", "motion");

            // 3. Debate (advanced) — two sides argue for up to two rounds, the judge rules.
            var pro = agent(Agents.DogAdvocate.class, model, "DogAdvocate", null);
            var con = agent(Agents.HouseholdAdvocate.class, model, "HouseholdAdvocate", null);
            var judge = agent(Agents.PackJudge.class, model, "PackJudge", "verdict");
            UntypedAgent debate = AgenticServices.plannerBuilder()
                    .subAgents(pro, con, judge)          // judge LAST
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .build();

            // 4. Glue (simple): the voters read 'text', the debate wrote 'verdict'.
            var note = agent(Agents.CouncilNote.class, model, "CouncilNote", "text");

            // 5. Voting (advanced) — three independent reads, majority ratifies.
            var a = agent(Agents.MoodSnifferA.class, model, "MoodSnifferA", null);
            var b = agent(Agents.MoodSnifferB.class, model, "MoodSnifferB", null);
            var c = agent(Agents.MoodSnifferC.class, model, "MoodSnifferC", null);
            UntypedAgent ratify = AgenticServices.plannerBuilder()
                    .subAgents(a, b, c)
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
                    "angles", List.of("health and safety — " + input,
                            "the household's routine — " + input,
                            "what Zao himself would choose — " + input)));
            return result(r, "verdict");
        };

        return new PatternDef("packCouncil", "Pack Council (composite)", "composite",
                "Settles a contested question: a mapper scouts three angles at once, one agent "
                        + "turns them into a motion, a debate argues it to a verdict, and a vote "
                        + "ratifies it. Two zoo patterns carried by simple plumbing.",
                // caveat: the zoo patterns are the easy part; the adapters between them are not.
                "Most of this system is glue. Each zoo pattern expects its input under its own key, "
                        + "so composing them is mostly writing the small steps in between.",
                topo, "should Zao be allowed on the sofa in the evening?", runner);
    }
}
