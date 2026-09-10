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
import static java.util.stream.Collectors.joining;

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

    // 1 — single agent: the kennel's front desk, doing one job
    private static PatternDef single() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "note", "input"),
                        node("clerk", "IntakeClerk", "agent")),
                List.of(edge("in", "clerk")));
        Runner runner = (model, input, listener) -> {
            var clerk = agent(Agents.IntakeClerk.class, model, "IntakeClerk", "record");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk).outputKey("record").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("note", input));
            return result(r, "record");
        };
        return new PatternDef("single", "Single Agent", "workflow",
                "One LLM call wrapped as an agent — the simplest useful unit. Here it does the "
                        + "job an LLM is genuinely best at: turning something a human said at a "
                        + "door into a shape a system can use.",
                "No decomposition: one agent struggles with multi-step or long tasks — and it "
                        + "will happily invent a dose that was never in the note.",
                topo,
                // A real drop-off note: abbreviated, out of order, one thing missing on purpose
                // (nobody said what he eats), so the room can check whether the agent obeys
                // "write 'not given'" or quietly makes something up.
                "dropping nero off til tues, big black gsd 40kg, half an antibiotic tablet "
                        + "morning + night WITH food, hates other males so never past the runs "
                        + "on the left, back tues after 5",
                runner);
    }

    // 2 — sequential: extract, then write for a different reader
    private static PatternDef sequential() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "note", "input"),
                        node("clerk", "IntakeClerk", "agent"),
                        node("sheet", "RunSheetWriter", "agent")),
                List.of(edge("in", "clerk"), edge("clerk", "sheet", "record")));
        Runner runner = (model, input, listener) -> {
            var clerk = agent(Agents.IntakeClerk.class, model, "IntakeClerk", "record");
            var sheet = agent(Agents.RunSheetWriter.class, model, "RunSheetWriter", "runSheet");
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk, sheet).outputKey("runSheet").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("note", input));
            return result(r, "runSheet");
        };
        return new PatternDef("sequential", "Sequential", "workflow",
                "Deterministic pipeline: each agent's output feeds the next. The second step "
                        + "cannot start before the first — it needs the structured record — and "
                        + "it writes for a different reader, which is why it is a second agent "
                        + "and not a longer prompt.",
                "Rigid order; a failure or bad hand-off midway derails the whole chain. Watch the "
                        + "Scope tab: 'record' is the seam, and everything downstream trusts it.",
                topo,
                "dropping nero off til tues, big black gsd 40kg, half an antibiotic tablet "
                        + "morning + night WITH food, hates other males so never past the runs "
                        + "on the left, back tues after 5",
                runner);
    }

    // 3 — loop: refine until FOUR NAMED RULES hold
    private static PatternDef loop() {
        Topology.Graph topo = graph("loop",
                List.of(node("in", "draft", "input"),
                        node("writer", "DischargeWriter", "agent"),
                        node("checker", "DischargeChecker", "agent")),
                List.of(edge("in", "writer"), edge("writer", "checker", "draft"),
                        edge("checker", "writer", "score < 0.8")));
        Runner runner = (model, input, listener) -> {
            var writer = agent(Agents.DischargeWriter.class, model, "DischargeWriter", "draft");
            var checker = agent(Agents.DischargeChecker.class, model, "DischargeChecker", "score");
            Predicate<AgenticScope> good = s -> score(s) >= 0.8;
            UntypedAgent app = AgenticServices.loopBuilder()
                    .subAgents(writer, checker)
                    .maxIterations(5)
                    .exitCondition(good)
                    .testExitAtLoopEnd(true)
                    .outputKey("draft")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("draft", input));
            return result(r, "draft");
        };
        return new PatternDef("loop", "Loop / Iterative Refinement", "workflow",
                "Refine until a quality bar is met. The bar is four rules the room can check "
                        + "too — every medicine named with dose and times, under 90 words, no "
                        + "jargon, the phone line last — so the score is a fraction of rules "
                        + "satisfied rather than a taste judgement, and you can see which one "
                        + "each pass fixes.",
                "Can spin forever or oscillate — always cap iterations and define a clear exit. A "
                        + "critic scoring 'quality' out of 1.0 gives you a number nobody can act "
                        + "on; score against named rules instead.",
                topo,
                // Deliberately bad on three of the four rules: jargon, no doses, no phone line.
                // The audience can count the failures before the first agent even runs.
                "Continue analgesia BID PRN and monitor the surgical site for dehiscence; "
                        + "maintain NPO after 20:00 and restrict ambulation to lead-only. "
                        + "Antibiosis as dispensed. Contact the practice should concerns arise.",
                runner);
    }

    // 4 — parallel: two independent checks, and a join that DECIDES
    private static PatternDef parallel() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "booking", "input"),
                        node("capacity", "CapacityCheck", "agent"),
                        node("health", "HealthCheck", "agent"),
                        // The combiner is the whole second half of "fan out, then join" — and
                        // here it is a rule, not a concatenation: any FAIL declines the booking.
                        node("join", "any FAIL declines", "join")),
                List.of(edge("in", "capacity"), edge("in", "health"),
                        edge("capacity", "join", "capacity"), edge("health", "join", "health")));
        Runner runner = (model, input, listener) -> {
            var capacity = agent(Agents.CapacityCheck.class, model, "CapacityCheck", "capacity");
            var health = agent(Agents.HealthCheck.class, model, "HealthCheck", "health");
            UntypedAgent app = AgenticServices.parallelBuilder()
                    .subAgents(capacity, health)
                    // The decision is plain Java over what the two agents wrote. Nothing about
                    // "did both checks pass" needs a model, and putting it in one keeps a demo
                    // honest about where the judgement actually lives.
                    .output(s -> {
                        String cap = str(s, "capacity");
                        String hea = str(s, "health");
                        boolean declined = (cap + " " + hea).toUpperCase(java.util.Locale.ROOT)
                                .contains("FAIL");
                        return (declined ? "Booking DECLINED" : "Booking ACCEPTED")
                                + "\n\n- Capacity: " + cap + "\n- Paperwork and medication: " + hea;
                    })
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("booking", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("parallel", "Parallel", "workflow",
                "Fan out independent work concurrently, then join. Neither check needs the "
                        + "other's answer, but the kennel cannot reply until both are in — which "
                        + "is exactly when fan-out-and-join is the right shape.",
                "Only for truly independent sub-tasks; joining is on you — and the join is where "
                        + "the real rule lives, so keep it in Java where you can test it.",
                topo,
                // Capacity is fine, paperwork is not: the run declines for a checkable reason
                // rather than producing two paragraphs nobody can grade.
                "Nero, 40kg male German shepherd, 12–19 October, rabies booster expired 12 June, "
                        + "half an antibiotic tablet twice a day",
                runner);
    }

    // 5 — parallel mapper: the same inspection over every occupied run
    private static PatternDef parallelMapper() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "runs[3]", "input"),
                        node("inspector", "RunInspector (per run)", "agent"),
                        node("gather", "watch-list", "join")),
                List.of(edge("in", "inspector", "scatter"),
                        edge("inspector", "gather", "flags")));
        Runner runner = (model, input, listener) -> {
            // The mapper collects each per-item invocation under the agent's outputKey, and binds
            // the item itself to the sub-agent's first argument.
            var inspector = agent(Agents.RunInspector.class, model, "RunInspector", "flag");
            UntypedAgent app = AgenticServices.parallelMapperBuilder()
                    .subAgents(inspector)
                    .itemsProvider("runs")
                    .outputKey("flags")
                    .listener(listener)
                    .build();
            // The items come from what the user typed (comma- or semicolon-separated), not a
            // hard-coded list — otherwise the input box on the page has no effect here.
            var r = app.invokeWithAgenticScope(Map.of("runs", items(input)));
            // Rendered as a list rather than String.valueOf(List): the gathered value really is
            // the shift's watch-list, and "[a, b, c]" on a projector reads as a Java toString
            // instead of the artefact the pattern just produced.
            Object flags = r.agenticScope() == null ? null : r.agenticScope().readState("flags");
            if (flags instanceof java.util.Collection<?> c) {
                return c.stream().map(String::valueOf).collect(joining("\n- ", "- ", ""));
            }
            return result(r, "flags");
        };
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "Map one agent over a collection in parallel (scatter/gather). The morning round: "
                        + "one inspection per occupied run, gathered into the shift's watch-list. "
                        + "The width of the fan-out is data, decided at run time.",
                "Beware fan-out cost and rate limits when the list is large — this is the pattern "
                        + "where a full kennel quietly becomes thirty concurrent calls.",
                topo,
                "Run 2 — Nero, left his supper, panting at 03:00; "
                        + "Run 5 — Luna, chewed her bedding, no stool overnight; "
                        + "Run 7 — Zao, slept through, ate everything",
                runner);
    }

    // 6 — conditional routing: the out-of-hours line, where mis-routing is dangerous
    private static PatternDef conditional() {
        Topology.Graph topo = graph("branch",
                List.of(node("in", "call", "input"),
                        node("router", "NightLineRouter", "router"),
                        node("emergency", "EmergencyVet", "agent"),
                        node("behaviour", "BehaviourDesk", "agent"),
                        node("booking", "BookingDesk", "agent")),
                List.of(edge("in", "router"),
                        edge("router", "emergency", "emergency"),
                        edge("router", "behaviour", "behaviour"),
                        edge("router", "booking", "booking")));
        Runner runner = (model, input, listener) -> {
            var router = agent(Agents.NightLineRouter.class, model, "NightLineRouter", "category");
            var emergency = agent(Agents.EmergencyVet.class, model, "EmergencyVet", "answer");
            var behaviour = agent(Agents.BehaviourDesk.class, model, "BehaviourDesk", "answer");
            var booking = agent(Agents.BookingDesk.class, model, "BookingDesk", "answer");
            Predicate<AgenticScope> isEmergency = s -> category(s).equals("emergency");
            Predicate<AgenticScope> isBehaviour = s -> category(s).equals("behaviour");
            Predicate<AgenticScope> isBooking = s -> category(s).equals("booking");
            UntypedAgent routed = AgenticServices.conditionalBuilder()
                    .subAgents(isEmergency, emergency)
                    .subAgents(isBehaviour, behaviour)
                    .subAgents(isBooking, booking)
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(router, routed)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("call", input));
            return result(r, "answer");
        };
        return new PatternDef("conditional", "Conditional Routing", "workflow",
                "A router classifies the input and dispatches to the right specialist. Worth it "
                        + "when mis-routing is expensive: this is the out-of-hours line, and a "
                        + "dog with bloat sent to the booking desk is dead by morning.",
                "Only as good as the classifier, and unseen categories fall through the cracks — "
                        + "so choose which way it falls. The fallback here is the emergency desk, "
                        + "because that is the mistake you can survive.",
                topo,
                // Textbook bloat (GDV). Anyone in the room who owns a large dog knows the right
                // answer, which is what makes the classifier's choice judgeable on stage.
                "Nero's belly has gone swollen and tight and he keeps trying to be sick but "
                        + "nothing comes up",
                runner);
    }
}
