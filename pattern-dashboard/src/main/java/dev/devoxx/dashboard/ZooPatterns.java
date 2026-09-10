package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.patterns.bdi.BDIPlanner;
import dev.langchain4j.agentic.patterns.bdi.Desire;
import dev.langchain4j.agentic.patterns.blackboard.BlackboardPlanner;
import dev.langchain4j.agentic.patterns.blackboard.ConflictResolutionStrategy;
import dev.langchain4j.agentic.patterns.debate.ConvergenceStrategy;
import dev.langchain4j.agentic.patterns.debate.DebatePlanner;
import dev.langchain4j.agentic.patterns.goap.GoalOrientedPlanner;
import dev.langchain4j.agentic.patterns.p2p.P2PPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingPlanner;
import dev.langchain4j.agentic.patterns.voting.VotingStrategy;
import dev.langchain4j.agentic.scope.AgenticScope;

/** The pattern zoo — planners from {@code langchain4j-agentic-patterns}, each with its own
 * idea of how agents should take turns. Experimental, and the most fun to watch. */
final class ZooPatterns {

    private ZooPatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(goap(), p2p(), blackboard(), voting(), debate(), bdi());
    }

    // 8 — GOAP (goal-oriented planning; outputKeys chain automatically)
    private static PatternDef goap() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "prompt", "input"),
                        node("extractor", "DogExtractor", "agent"),
                        node("bio", "PackBiographer", "agent")),
                List.of(edge("in", "extractor"), edge("extractor", "bio", "dog")));
        Runner runner = (model, input, listener) -> {
            var extractor = agent(Agents.DogExtractor.class, model, "DogExtractor", "dog");
            var bio = agent(Agents.PackBiographer.class, model, "PackBiographer", "writeup");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(extractor, bio)
                    .planner(GoalOrientedPlanner::new)
                    .outputKey("writeup")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("prompt", input));
            return result(r, "writeup");
        };
        return new PatternDef("goap", "GOAP (Goal-Oriented Planning)", "pattern-zoo",
                "The planner orders agents automatically by matching each output to the next input.",
                // caveat: planning is only as good as the declared pre/post-conditions (I/O keys).
                "Needs well-declared I/O keys; a missing link means the goal is unreachable.",
                topo, "Write a short bio: Zao, the famous Belgian shepherd of the Ardennes",
                runner);
    }

    // 9 — P2P (peers refine shared state until consensus)
    private static PatternDef p2p() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "issue", "input"),
                        node("negotiator", "PackNegotiator", "agent"),
                        node("mediator", "PackMediator", "agent")),
                List.of(edge("in", "negotiator"),
                        edge("negotiator", "mediator", "proposal"),
                        edge("mediator", "negotiator", "refine")));
        Runner runner = (model, input, listener) -> {
            var negotiator = agent(Agents.PackNegotiator.class, model, "PackNegotiator", "proposal");
            var mediator = agent(Agents.PackMediator.class, model, "PackMediator", "consensus");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(negotiator, mediator)
                    .planner(() -> new P2PPlanner(10, s -> s.hasState("consensus")))
                    .outputKey("consensus")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("issue", input));
            return result(r, "consensus");
        };
        return new PatternDef("p2p", "Peer-to-Peer", "pattern-zoo",
                "Peers iteratively refine a shared blackboard until an exit condition holds.",
                // caveat: without a firm exit predicate peers can ping-pong indefinitely.
                "No fixed hierarchy — needs a solid exit predicate or it never terminates.",
                topo, "should Zao sleep indoors or outside with the rest of the pack?", runner);
    }

    // 10 — blackboard (experts contribute until goal state is reached)
    private static PatternDef blackboard() {
        Topology.Graph topo = graph("star",
                // The only pattern that still draws the shared state: here it is not plumbing,
                // it is the pattern. Every other topology dropped its AgenticScope sink — it was
                // the same box in all 13 diagrams, and the scope now has its own tab.
                List.of(node("scope", "Blackboard", "board"),
                        node("tracker", "Tracker", "agent"),
                        node("analyst", "PackAnalyst", "agent"),
                        node("leader", "PackLeader", "agent")),
                // Experts read the board as well as write to it — that mutual dependency is why
                // the pattern needs a conflict-resolution strategy at all.
                List.of(edge("tracker", "scope", "facts"), edge("scope", "tracker"),
                        edge("analyst", "scope", "analysis"), edge("scope", "analyst"),
                        edge("leader", "scope", "solution"), edge("scope", "leader")));
        Runner runner = (model, input, listener) -> {
            var tracker = agent(Agents.Tracker.class, model, "Tracker", "facts");
            var analyst = agent(Agents.PackAnalyst.class, model, "PackAnalyst", "analysis");
            var leader = agent(Agents.PackLeader.class, model, "PackLeader", "solution");
            Predicate<AgenticScope> goal = s -> s.hasState("solution");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(tracker, analyst, leader)
                    .planner(() -> new BlackboardPlanner(goal,
                            ConflictResolutionStrategy.declarationOrder()))
                    .outputKey("solution")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("problem", input));
            return result(r, "solution");
        };
        return new PatternDef("blackboard", "Blackboard", "pattern-zoo",
                "Experts read/write a shared blackboard, contributing until a goal state exists.",
                // caveat: concurrent writers need a conflict-resolution strategy.
                "Shared mutable state invites conflicts; pick a conflict-resolution strategy.",
                topo, "how to keep Zao happy and healthy through a Belgian winter", runner);
    }

    // 11 — voting (3 classifiers, majority wins)
    private static PatternDef voting() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "text", "input"),
                        node("a", "MoodSnifferA", "agent"),
                        node("b", "MoodSnifferB", "agent"),
                        node("c", "MoodSnifferC", "agent"),
                        // Without the tally this is just a fan-out; the tally IS the pattern.
                        node("vote", "majority()", "join")),
                List.of(edge("in", "a"), edge("in", "b"), edge("in", "c"),
                        edge("a", "vote"), edge("b", "vote"), edge("c", "vote")));
        Runner runner = (model, input, listener) -> {
            var a = agent(Agents.MoodSnifferA.class, model, "MoodSnifferA", null);
            var b = agent(Agents.MoodSnifferB.class, model, "MoodSnifferB", null);
            var c = agent(Agents.MoodSnifferC.class, model, "MoodSnifferC", null);
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(a, b, c)
                    .planner(() -> new VotingPlanner(VotingStrategy.majority()))
                    .outputKey("classification")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("text", input));
            return result(r, "classification");
        };
        return new PatternDef("voting", "Voting / Ensemble", "pattern-zoo",
                "Several agents answer independently; a strategy aggregates (majority/avg/highest).",
                // caveat: correlated models vote alike, so an ensemble can be confidently wrong.
                "Correlated voters agree on the same mistake — diversity is what buys robustness.",
                topo, "Zao learned to open the fridge and ate the whole roast", runner);
    }

    // 12 — debate (2 debaters + judge; judge LAST)
    private static PatternDef debate() {
        Topology.Graph topo = graph("mesh",
                List.of(node("in", "motion", "input"),
                        node("a", "DogAdvocate", "agent"),
                        node("b", "HouseholdAdvocate", "agent"),
                        node("judge", "PackJudge", "judge")),
                List.of(edge("in", "a"), edge("in", "b"),
                        edge("a", "b", "rebut"), edge("b", "a", "rebut"),
                        edge("a", "judge"), edge("b", "judge")));
        Runner runner = (model, input, listener) -> {
            var a = agent(Agents.DogAdvocate.class, model, "DogAdvocate", null);
            var b = agent(Agents.HouseholdAdvocate.class, model, "HouseholdAdvocate", null);
            var judge = agent(Agents.PackJudge.class, model, "PackJudge", "verdict");
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(a, b, judge) // last sub-agent is the judge
                    .planner(() -> new DebatePlanner(2, ConvergenceStrategy.unanimous()))
                    .outputKey("verdict")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("motion", input));
            return result(r, "verdict");
        };
        return new PatternDef("debate", "Debate", "pattern-zoo",
                "Agents argue opposing sides for N rounds; a judge renders the verdict.",
                // caveat: eloquence can beat correctness; more rounds cost more tokens.
                "The most persuasive agent may win over the most correct one — and it's token-hungry.",
                topo, "Zao should be allowed on the sofa", runner);
    }

    // 13 — BDI (beliefs-desires-intentions; prioritised desires)
    private static PatternDef bdi() {
        Topology.Graph topo = graph("dag",
                List.of(node("in", "goal", "input"),
                        node("gatherer", "CareScout (desire p10)", "agent"),
                        node("reporter", "CareReporter (desire p5)", "agent")),
                List.of(edge("in", "gatherer"),
                        edge("gatherer", "reporter", "info")));
        Runner runner = (model, input, listener) -> {
            var gatherer = agent(Agents.CareScout.class, model, "CareScout", "info");
            var reporter = agent(Agents.CareReporter.class, model, "CareReporter", "report");
            List<Desire> desires = List.of(
                    Desire.of("gather-info", 10, s -> true, s -> s.hasState("info"),
                            Agents.CareScout.class),
                    Desire.of("write-report", 5, s -> s.hasState("info"), s -> s.hasState("report"),
                            Agents.CareReporter.class));
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(gatherer, reporter)
                    .planner(() -> new BDIPlanner(desires))
                    .outputKey("report")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("goal", input));
            return result(r, "report");
        };
        return new PatternDef("bdi", "BDI (Belief-Desire-Intention)", "pattern-zoo",
                "Agent pursues prioritised desires, acting on the highest achievable, unmet one.",
                // caveat: designing achievable/satisfied predicates is subtle and easy to get wrong.
                "Powerful but fiddly: the achievable/satisfied predicates are hard to get right.",
                topo, "produce a winter care report for the dog Zao", runner);
    }
}
