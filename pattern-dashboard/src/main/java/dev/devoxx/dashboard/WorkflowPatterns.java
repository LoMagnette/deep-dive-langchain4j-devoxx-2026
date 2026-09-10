package dev.devoxx.dashboard;

import static dev.devoxx.dashboard.Parsing.category;
import static dev.devoxx.dashboard.Parsing.items;
import static dev.devoxx.dashboard.Parsing.score;
import static dev.devoxx.dashboard.Topology.edge;
import static dev.devoxx.dashboard.Topology.graph;
import static dev.devoxx.dashboard.Topology.node;
import static dev.devoxx.dashboard.Wiring.agent;
import static dev.devoxx.dashboard.Wiring.result;
import static dev.devoxx.dashboard.Wiring.str;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import dev.devoxx.dashboard.PatternDef.Runner;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;

/** Deterministic plumbing: the shape of the graph is fixed before the first token. */
final class WorkflowPatterns {

    private WorkflowPatterns() {
    }

    /** In the order the rail and the gallery show them. */
    static List<PatternDef> all() {
        return List.of(single(), sequential(), loop(), parallel(), parallelMapper(), conditional());
    }

    // 1 — single agent
    private static PatternDef single() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "topic", "input"),
                        node("writer", "PackChronicler", "agent")),
                List.of(edge("in", "writer")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(Agents.PackChronicler.class, model, "PackChronicler", "tale");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(writer).outputKey("tale").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("topic", input));
            return result(r, "tale");
        };
        return new PatternDef("single", "Single Agent", "workflow",
                "One LLM call wrapped as an agent — the simplest useful unit.",
                "No decomposition: a single agent struggles with multi-step or long tasks.",
                topo, "Zao the Belgian shepherd meets a wolf pack at dawn in the Ardennes", runner);
    }

    // 2 — sequential (writer -> editor)
    private static PatternDef sequential() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "topic", "input"),
                        node("writer", "PackChronicler", "agent"),
                        node("editor", "PackEditor", "agent")),
                List.of(edge("in", "writer"), edge("writer", "editor", "tale")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(Agents.PackChronicler.class, model, "PackChronicler", "tale");
            var editor = agent(Agents.PackEditor.class, model, "PackEditor", "editedTale");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(writer, editor).outputKey("editedTale").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("topic", input));
            return result(r, "editedTale");
        };
        return new PatternDef("sequential", "Sequential", "workflow",
                "Deterministic pipeline: each agent's output feeds the next.",
                "Rigid order; a failure or bad hand-off midway derails the whole chain.",
                topo, "Zao steals a whole speculoos cake and hides under the table", runner);
    }

    // 3 — loop (editor + scorer until score >= 0.8)
    private static PatternDef loop() {
        Topology.Graph topo = graph("loop",
                List.of(node("in", "tale", "input"),
                        node("editor", "PackEditor", "agent"),
                        node("scorer", "PackCritic", "agent")),
                List.of(edge("in", "editor"), edge("editor", "scorer", "tale"),
                        edge("scorer", "editor", "score < 0.8")));
        Runner runner = (model, input, listener) -> {
            var editor = agent(Agents.PackEditor.class, model, "PackEditor", "tale");
            var scorer = agent(Agents.PackCritic.class, model, "PackCritic", "score");
            Predicate<AgenticScope> good = s -> score(s) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(editor, scorer)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("tale")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("tale", input));
            return result(r, "tale");
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "Refine until a quality bar is met (self-critique with an exit condition).",
                "Can spin forever or oscillate — always cap iterations and define a clear exit.",
                topo, "Zao is elected leader of the Ardennes wolf pack", runner);
    }

    // 4 — parallel (movie + meal, combined)
    private static PatternDef parallel() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "mood", "input"),
                        node("walk", "WalkExpert", "agent"),
                        node("treat", "TreatExpert", "agent"),
                        // The combiner is the whole second half of "fan out, then join".
                        node("join", "combine", "join")),
                List.of(edge("in", "walk"), edge("in", "treat"),
                        edge("walk", "join", "walk"), edge("treat", "join", "treat")));
        Runner runner = (model, input, listener) -> {
            var walk = agent(Agents.WalkExpert.class, model, "WalkExpert", "walk");
            var treat = agent(Agents.TreatExpert.class, model, "TreatExpert", "treat");
            UntypedAgent app = AgenticServices.parallelBuilder()
                    .subAgents(walk, treat)
                    .output(s -> "Walk: " + str(s, "walk") + "\nTreat: " + str(s, "treat"))
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("mood", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("parallel", "Parallel", "workflow",
                "Fan out independent work concurrently, then join the results.",
                "Only for truly independent sub-tasks; joining/merging logic is on you.",
                topo, "restless and bored after three days of Belgian rain", runner);
    }

    // 5 — parallel mapper (one agent over a list of items)
    private static PatternDef parallelMapper() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "topics[3]", "input"),
                        node("scout", "PackScout (per item)", "agent"),
                        node("gather", "gather", "join")),
                List.of(edge("in", "scout", "scatter"), edge("scout", "gather", "findings")));
        Runner runner = (model, input, listener) -> {
            // The mapper collects each per-item invocation under the agent's outputKey.
            var scout = agent(Agents.PackScout.class, model, "PackScout", "finding");
            UntypedAgent app = AgenticServices.parallelMapperBuilder()
                    .subAgents(scout)
                    .itemsProvider("topics")
                    .outputKey("findings")
                    .listener(listener)
                    .build();
            // The items come from what the user typed (comma-separated), not a hard-coded list —
            // otherwise the input box on the page has no effect on this pattern.
            var r = app.invokeWithAgenticScope(Map.of("topics", items(input)));
            return result(r, "findings");
        };
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "Map one agent over a collection in parallel (scatter/gather).",
                "Beware fan-out cost and rate limits when the list is large.",
                topo, "Zao's favourite chew toys, wolf packs in the Ardennes, "
                        + "why dogs howl at sirens", runner);
    }

    // 6 — conditional routing
    private static PatternDef conditional() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "request", "input"),
                        node("router", "KennelRouter", "router"),
                        node("behaviour", "BehaviourExpert", "agent"),
                        node("nutrition", "NutritionExpert", "agent"),
                        node("vet", "VetExpert", "agent")),
                List.of(edge("in", "router"),
                        edge("router", "behaviour", "behaviour"),
                        edge("router", "nutrition", "nutrition"),
                        edge("router", "vet", "veterinary")));
        Runner runner = (model, input, listener) -> {
            var router = agent(Agents.KennelRouter.class, model, "KennelRouter", "category");
            var behaviour = agent(Agents.BehaviourExpert.class, model, "BehaviourExpert", "answer");
            var nutrition = agent(Agents.NutritionExpert.class, model, "NutritionExpert", "answer");
            var vet = agent(Agents.VetExpert.class, model, "VetExpert", "answer");
            Predicate<AgenticScope> isBehaviour = s -> category(s).equals("behaviour");
            Predicate<AgenticScope> isNutrition = s -> category(s).equals("nutrition");
            Predicate<AgenticScope> isVet = s -> category(s).equals("veterinary");
            UntypedAgent routed = AgenticServices.conditionalBuilder()
                    .subAgents(isBehaviour, behaviour)
                    .subAgents(isNutrition, nutrition)
                    .subAgents(isVet, vet)
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, routed)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("request", input));
            return result(r, "answer");
        };
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "A router classifies the input and dispatches to the right specialist.",
                "Only as good as the classifier; unseen categories fall through the cracks.",
                topo, "Zao keeps limping after long walks in the woods — what should I do?",
                runner);
    }
}
