package dev.devoxx.dashboard.demos.parallelmapper;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.items;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.PatternDef.Runner;
import dev.devoxx.dashboard.catalog.Topology;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;

/**
 * Wiring for the <b>parallel mapper</b> demo — the same check over everything he got hold of.
 */
public final class ParallelMapperPattern {

    private ParallelMapperPattern() {
    }

    public static PatternDef define() {
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "he ate[5]", "input"),
                        node("check", "FoodSafetyCheck (per item)", "agent"),
                        node("gather", "one verdict each", "join")),
                List.of(edge("in", "check", "scatter"),
                        edge("check", "gather", "verdicts")));
        Runner runner = (model, input, listener) -> {
            // The mapper collects each per-item invocation under the agent's outputKey, and binds
            // the item itself to the sub-agent's first argument.
            var check = AgenticServices.agentBuilder(FoodSafetyCheck.class)
                    .chatModel(model)
                    .name("FoodSafetyCheck")
                    .outputKey("verdict")
                    .build();
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
            return String.valueOf(r.result());
        };
        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                // The beat this demo plays in the running narration.
                "The picnic. Five things off the blanket before anybody noticed.",
                // What this demo inherits from the ones before it.
                null,
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
}
