package dev.devoxx.dashboard.catalog;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.category;
import static dev.devoxx.dashboard.support.Parsing.items;
import static dev.devoxx.dashboard.support.Parsing.score;
import static dev.devoxx.dashboard.support.Wiring.agent;
import static dev.devoxx.dashboard.support.Wiring.result;
import static dev.devoxx.dashboard.support.Wiring.str;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.agents.workflow.DogCheck;
import dev.devoxx.dashboard.agents.workflow.DogTrainer;
import dev.devoxx.dashboard.agents.workflow.EmergencyVet;
import dev.devoxx.dashboard.agents.workflow.EverydayCare;
import dev.devoxx.dashboard.agents.workflow.FoodSafetyCheck;
import dev.devoxx.dashboard.agents.workflow.FridgeChecklist;
import dev.devoxx.dashboard.agents.workflow.FridgeRuleCheck;
import dev.devoxx.dashboard.agents.workflow.SitterCardClerk;
import dev.devoxx.dashboard.agents.workflow.SitterNoteWriter;
import dev.devoxx.dashboard.agents.workflow.WeatherCheck;
import dev.devoxx.dashboard.agents.workflow.WorryRouter;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
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

    /** The message every household sends the friend who is watching the dog. */
    private static final String SITTER_MESSAGE =
            "hey so thanks again for having zao!! he's the big black belgian shepherd, food's "
                    + "in the tub by the back door he has two scoops morning and evening, oh and "
                    + "he CANNOT have the dried liver treats anymore they upset him. don't let "
                    + "him off the lead in the park he won't come back yet. vet is 061 22 33 44 "
                    + "if anything happens. he'll cry the first night, ignore it, he's fine!!";

    // 1 — single agent: one call, one job
    private static PatternDef single() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "message", "input"),
                        node("clerk", "SitterCardClerk", "agent")),
                List.of(edge("in", "clerk")));
        Runner runner = (model, input, listener) -> {
            var clerk = agent(SitterCardClerk.class, model, "SitterCardClerk", "card");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk).outputKey("card").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("message", input));
            return result(r, "card");
        };
        return new PatternDef("single", "Single Agent", "workflow",
                "One LLM call wrapped as an agent — the simplest useful unit, doing the job an "
                        + "LLM is genuinely best at: turning what a human actually typed into a "
                        + "shape a system can use.",
                "No decomposition: one agent struggles with multi-step or long tasks — and watch "
                        + "the Walks line, because a model would rather invent a walk time than "
                        + "admit the message never gave one.",
                topo,
                // A real message: no punctuation, out of order, and one field genuinely absent
                // (nobody said when to walk him), so the room can check whether the agent obeys
                // "write not given" or quietly makes something up.
                SITTER_MESSAGE,
                runner);
    }

    // 2 — sequential: the same facts, rewritten for a different reader
    private static PatternDef sequential() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "message", "input"),
                        node("clerk", "SitterCardClerk", "agent"),
                        node("list", "FridgeChecklist", "agent")),
                List.of(edge("in", "clerk"), edge("clerk", "list", "card")));
        Runner runner = (model, input, listener) -> {
            var clerk = agent(SitterCardClerk.class, model, "SitterCardClerk", "card");
            var list = agent(FridgeChecklist.class, model, "FridgeChecklist", "checklist");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk, list).outputKey("checklist").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("message", input));
            return result(r, "checklist");
        };
        return new PatternDef("sequential", "Sequential", "workflow",
                "Deterministic pipeline: each agent's output feeds the next. The second step "
                        + "cannot start before the first — it needs the card — and it writes for "
                        + "a different reader, someone standing in your kitchen at 07:00. That is "
                        + "why it is a second agent and not a longer prompt.",
                "Rigid order; a bad hand-off midway derails the whole chain. Watch the Scope tab: "
                        + "'card' is the seam, and the second agent trusts it completely.",
                topo, SITTER_MESSAGE, runner);
    }

    // 3 — loop: refine until four rules the room agrees with are satisfied
    private static PatternDef loop() {
        Topology.Graph topo = graph("loop",
                List.of(node("in", "note", "input"),
                        node("writer", "SitterNoteWriter", "agent"),
                        node("check", "FridgeRuleCheck", "agent")),
                List.of(edge("in", "writer"), edge("writer", "check", "note"),
                        edge("check", "writer", "score < 0.8")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(SitterNoteWriter.class, model, "SitterNoteWriter", "note");
            var check = agent(FridgeRuleCheck.class, model, "FridgeRuleCheck", "score");
            Predicate<AgenticScope> good = s -> score(s) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(writer, check)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("note")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("note", input));
            return result(r, "note");
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "Refine until a quality bar is met. The bar is four rules nobody has to be "
                        + "persuaded of — every meal with a time and an amount, where the lead "
                        + "is, the vet's number, short enough for the fridge door — so the score "
                        + "is a fraction of rules satisfied, and you can see which one each pass "
                        + "fixes.",
                "Can spin forever or oscillate — always cap iterations and define a clear exit. A "
                        + "critic scoring 'quality' out of 1.0 gives you a number nobody in the "
                        + "room can check; score against named rules instead.",
                topo,
                // Fails three of the four rules on sight, which is the point: the audience can
                // count the failures before the first agent runs.
                "just feed him twice like normal and take him out when you can, he knows the "
                        + "routine. ring me if anything's up!",
                runner);
    }

    // 4 — parallel: two independent checks, and a join that DECIDES
    private static PatternDef parallel() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "right now", "input"),
                        node("weather", "WeatherCheck", "agent"),
                        node("dog", "DogCheck", "agent"),
                        // The combiner is the whole second half of "fan out, then join" — and
                        // here it is a rule, not a concatenation: either check can veto the walk.
                        node("join", "either can veto", "join")),
                List.of(edge("in", "weather"), edge("in", "dog"),
                        edge("weather", "join", "weather"), edge("dog", "join", "dog")));
        Runner runner = (model, input, listener) -> {
            var weather = agent(WeatherCheck.class, model, "WeatherCheck", "weather");
            var dog = agent(DogCheck.class, model, "DogCheck", "dog");
            UntypedAgent app = AgenticServices.parallelBuilder()
                    .subAgents(weather, dog)
                    // The decision is plain Java over what the two agents wrote. Nothing about
                    // "did both checks pass" needs a model, and putting it in one would be a
                    // demo lying about where the judgement actually lives.
                    .output(s -> {
                        String w = str(s, "weather");
                        String d = str(s, "dog");
                        boolean veto = (w + " " + d).toUpperCase(Locale.ROOT).contains("FAIL");
                        return (veto ? "Not now" : "Fine — get the lead")
                                + "\n\n- Weather and ground: " + w + "\n- Zao himself: " + d;
                    })
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("walk", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("parallel", "Parallel", "workflow",
                "Fan out independent work concurrently, then join. The weather does not depend on "
                        + "the dog and the dog does not depend on the weather, but you cannot put "
                        + "the lead on until both have answered — which is exactly when "
                        + "fan-out-and-join is the right shape.",
                "Only for truly independent sub-tasks; joining is on you — and the join is where "
                        + "the real rule lives, so keep it in Java where you can test it.",
                topo,
                // Everyone in the room already knows the answer: not at two in the afternoon in
                // July. So they can grade the run instead of taking it on trust.
                "two o'clock on a July afternoon, 31 degrees, the pavement has been in the sun "
                        + "all day. Zao is four, he ate an hour ago, nothing else wrong with him",
                runner);
    }

    // 5 — parallel mapper: the same check over everything he got hold of
    private static PatternDef parallelMapper() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "he ate[5]", "input"),
                        node("check", "FoodSafetyCheck (per item)", "agent"),
                        node("gather", "one verdict each", "join")),
                List.of(edge("in", "check", "scatter"),
                        edge("check", "gather", "verdicts")));
        Runner runner = (model, input, listener) -> {
            // The mapper collects each per-item invocation under the agent's outputKey, and binds
            // the item itself to the sub-agent's first argument.
            var check = agent(FoodSafetyCheck.class, model, "FoodSafetyCheck", "verdict");
            UntypedAgent app = AgenticServices.parallelMapperBuilder()
                    .subAgents(check)
                    .itemsProvider("eaten")
                    .outputKey("verdicts")
                    .listener(listener)
                    .build();
            // The items come from what the user typed (comma- or semicolon-separated), not a
            // hard-coded list — otherwise the input box on the page has no effect here.
            List<String> eaten = items(input);
            var r = app.invokeWithAgenticScope(Map.of("eaten", eaten));
            // Each verdict is paired back with the item it is about. The mapper preserves order,
            // and String.valueOf(List) would put five unlabelled verdicts on the screen for the
            // room to match up by counting — which is exactly the moment the demo loses them.
            Object verdicts = r.agenticScope() == null ? null
                    : r.agenticScope().readState("verdicts");
            if (verdicts instanceof java.util.Collection<?> c) {
                List<String> said = c.stream().map(String::valueOf).toList();
                return IntStream.range(0, said.size())
                        .mapToObj(i -> "- **" + (i < eaten.size() ? eaten.get(i) : "item " + i)
                                + "** — " + said.get(i))
                        .collect(joining("\n"));
            }
            return result(r, "verdicts");
        };
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "Map one agent over a collection in parallel (scatter/gather). Five things off "
                        + "the picnic blanket, one verdict each. The width of the fan-out is "
                        + "data, decided at run time — and you already know all five answers, so "
                        + "you can mark this run yourself.",
                "Beware fan-out cost and rate limits when the list is long — this is the pattern "
                        + "where emptying a whole cupboard into the box quietly becomes fifty "
                        + "concurrent calls.",
                topo,
                "a handful of grapes; a slice of cheddar; a square of dark chocolate; "
                        + "a crust of bread; half a raw onion",
                runner);
    }

    // 6 — conditional routing: where sending it to the wrong person is the disaster
    private static PatternDef conditional() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "worry", "input"),
                        node("router", "WorryRouter", "router"),
                        node("vet", "EmergencyVet", "agent"),
                        node("trainer", "DogTrainer", "agent"),
                        node("care", "EverydayCare", "agent")),
                List.of(edge("in", "router"),
                        edge("router", "vet", "emergency"),
                        edge("router", "trainer", "training"),
                        edge("router", "care", "everyday")));
        Runner runner = (model, input, listener) -> {
            var router = agent(WorryRouter.class, model, "WorryRouter", "category");
            var vet = agent(EmergencyVet.class, model, "EmergencyVet", "answer");
            var trainer = agent(DogTrainer.class, model, "DogTrainer", "answer");
            var care = agent(EverydayCare.class, model, "EverydayCare", "answer");
            Predicate<AgenticScope> isEmergency = s -> category(s).equals("emergency");
            Predicate<AgenticScope> isTraining = s -> category(s).equals("training");
            Predicate<AgenticScope> isEveryday = s -> category(s).equals("everyday");
            UntypedAgent routed = AgenticServices.conditionalBuilder()
                    .subAgents(isEmergency, vet)
                    .subAgents(isTraining, trainer)
                    .subAgents(isEveryday, care)
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, routed)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("worry", input));
            return result(r, "answer");
        };
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "A router classifies the input and dispatches to the right specialist. Worth it "
                        + "when mis-routing is expensive: everyone in this room knows a dog that "
                        + "has eaten chocolate needs a vet and not a training tip, so everyone "
                        + "can see whether the classifier got it right.",
                "Only as good as the classifier, and unseen inputs fall through the cracks — so "
                        + "choose which way it falls. The fallback here is the vet, because that "
                        + "is the mistake you can live with.",
                topo,
                "he's just eaten a whole bar of dark chocolate off the coffee table, the wrapper "
                        + "is on the floor",
                runner);
    }
}
