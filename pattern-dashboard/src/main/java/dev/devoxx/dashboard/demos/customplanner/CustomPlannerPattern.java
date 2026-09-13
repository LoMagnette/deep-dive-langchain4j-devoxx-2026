package dev.devoxx.dashboard.demos.customplanner;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.demos.conditional.DogTrainer;
import dev.devoxx.dashboard.demos.conditional.EmergencyVet;
import dev.devoxx.dashboard.demos.conditional.EverydayCare;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgentInvocation;

/**
 * Wiring for the <b>custom planner</b> demo — the escalation ladder. See EscalationPlanner.
 */
public final class CustomPlannerPattern {

    private CustomPlannerPattern() {
    }

    /** The escalation ladder, cheapest first — the same order the planner is handed. */
    private static final List<String> LADDER =
            List.of("EverydayCare", "DogTrainer", "EmergencyVet");

    public static PatternDef define() {
        Topology.Graph topo = graph("stages",
                // The three tiers share one column, stacked, so escalation reads downwards and
                // each rung's own way out reads across. A plain chain would draw a pipeline that
                // always runs all three, which is the opposite of what this planner does.
                List.of(node("in", "worry", "input", 0),
                        node("book", "EverydayCare", "agent", 1),
                        node("trainer", "DogTrainer", "agent", 1),
                        node("vet", "EmergencyVet", "agent", 1),
                        node("out", "first ANSWERED wins", "join", 2)),
                List.of(edge("in", "book"),
                        edge("book", "trainer", "ESCALATE"),
                        edge("trainer", "vet", "ESCALATE"),
                        edge("book", "out", "ANSWERED"),
                        edge("trainer", "out"),
                        edge("vet", "out")));
        Runner runner = (model, input, listener) -> {
            // Declaration order IS the cost order — that is the whole configuration of this
            // planner, and it is worth pointing at on stage: no prompt says "cheapest first".
            var book = AgenticServices.agentBuilder(EverydayCare.class)
                    .chatModel(model)
                    .name("EverydayCare")
                    .outputKey("answer")
                    .build();
            var trainer = AgenticServices.agentBuilder(DogTrainer.class)
                    .chatModel(model)
                    .name("DogTrainer")
                    .outputKey("answer")
                    .build();
            var vet = AgenticServices.agentBuilder(EmergencyVet.class)
                    .chatModel(model)
                    .name("EmergencyVet")
                    .outputKey("answer")
                    .build();
            UntypedAgent app = AgenticServices.plannerBuilder()
                    .subAgents(book, trainer, vet)
                    // Same builder as every pattern above it. The only difference is that this
                    // planner is forty lines in this repo instead of forty lines in the library.
                    .planner(EscalationPlanner::new)
                    .outputKey("answer")
                    .listener(listener)
                    .build();
            var r = app.invokeWithAgenticScope(Map.of("worry", input));
            // Report WHICH rung settled it and how many were asked. Returning just the answer
            // would hide the only thing this pattern does differently from a sequence — the
            // scope's invocation history is what makes that reportable without threading state
            // out of the planner.
            String answer = String.valueOf(r.result());
            var scope = r.agenticScope();
            if (scope == null) {
                return answer;
            }
            List<String> asked = scope.agentInvocations().stream()
                    .map(AgentInvocation::agentName).filter(LADDER::contains).distinct().toList();
            boolean settled = answer.toUpperCase(Locale.ROOT).lastIndexOf("ANSWERED")
                    > answer.toUpperCase(Locale.ROOT).lastIndexOf("ESCALATE");
            String rung = asked.isEmpty() ? "nobody" : asked.get(asked.size() - 1);
            // The marker is protocol, not prose: the planner read it, the Scope tab still shows
            // it on the raw value, and the reader does not need it in the answer.
            String shown = answer.replaceAll("(?is)\\s*(ANSWERED|ESCALATE)\\s*$", "");
            return "**" + (settled ? "Answered by " + rung
                        : "Nobody could answer — best effort from " + rung)
                    + "** · asked " + asked.size() + " of " + LADDER.size() + " rungs\n\n" + shown;
        };
        return new PatternDef("customPlanner", "Custom Planner (write your own)", "pattern-zoo",
                // The beat this demo plays in the running narration.
                "By now you have learned who to ask, and in what order, before you ring "
                        + "anybody at all.",
                // What this demo inherits from the ones before it.
                "Demo 6's three desks a third time. Routing picks one, the supervisor "
                        + "picks several, and this tries them cheapest-first and stops early.",
                "Every planner above is an implementation of one small interface — here is one "
                        + "written by hand. The policy is a cost ladder: ask the book, then the "
                        + "trainer, then the vet, and stop at the first rung that can actually "
                        + "answer. Change the question and watch it stop at a different rung: "
                        + "that decision depends on what came back, which is the one thing none "
                        + "of the built-in builders can express.",
                // caveat: you own the control loop, including the ways it can fail to end.
                "You own the loop now. Nothing stops a planner from never terminating, calling "
                        + "the same agent forever, or spending the whole budget on the top rung — "
                        + "`terminated()` and a hard tier count are the guard rails, and they are "
                        + "yours to write. Reach for this only when a built-in builder genuinely "
                        + "cannot say what you mean.",
                topo,
                // Escalates all the way, so the default run walks the whole ladder. Try
                // "which food should I buy for a four-year-old shepherd?" and it stops at the
                // book; try "he pulls like a train on the lead" and it stops at the trainer.
                "he's suddenly limping on his back left leg and won't put weight on it",
                runner);
    }
}
