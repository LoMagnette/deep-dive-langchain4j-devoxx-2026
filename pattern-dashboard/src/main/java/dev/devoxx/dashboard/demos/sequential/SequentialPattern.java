package dev.devoxx.dashboard.demos.sequential;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.demos.single.SinglePattern.SITTER_MESSAGE;

import java.util.List;
import java.util.Map;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.single.SitterCardClerk;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * Wiring for the <b>sequential</b> demo — the same facts, rewritten for a different reader.
 */
public final class SequentialPattern {

    private SequentialPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("chain",
                List.of(node("in", "message", "input"),
                        node("clerk", "SitterCardClerk", "agent"),
                        node("list", "FridgeChecklist", "agent")),
                List.of(edge("in", "clerk"), edge("clerk", "list", "card")));
        Runner runner = (model, input, listener) -> {
            var clerk = AgenticServices.agentBuilder(SitterCardClerk.class)
                    .chatModel(model)
                    .name("SitterCardClerk")
                    .outputKey("card")
                    .build();
            var list = AgenticServices.agentBuilder(FridgeChecklist.class)
                    .chatModel(model)
                    .name("FridgeChecklist")
                    .outputKey("checklist")
                    .build();
            UntypedAgent app = AgenticServices.sequenceBuilder()
                    .subAgents(clerk, list).outputKey("checklist").listener(listener).build();
            var r = app.invokeWithAgenticScope(Map.of("message", input));
            return String.valueOf(r.result());
        };
        return new PatternDef("sequential", "Sequential", "workflow",
                // The beat this demo plays in the running narration.
                "They need it on the fridge door, not buried in their phone.",
                "Deterministic pipeline: each agent's output feeds the next. The second step "
                        + "cannot start before the first — it needs the card — and it writes for "
                        + "a different reader, someone standing in your kitchen at 07:00. That is "
                        + "why it is a second agent and not a longer prompt.",
                "Rigid order; a bad hand-off midway derails the whole chain. Watch the Scope tab: "
                        + "'card' is the seam, and the second agent trusts it completely.",
                topo, SITTER_MESSAGE, runner);
    }
}
