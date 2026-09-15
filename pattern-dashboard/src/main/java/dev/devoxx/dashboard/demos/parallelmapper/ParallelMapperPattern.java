package dev.devoxx.dashboard.demos.parallelmapper;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.items;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.demos.debate.Keys.Verdict;
import dev.devoxx.dashboard.demos.parallelmapper.Keys.Eaten;
import dev.devoxx.dashboard.demos.parallelmapper.Keys.Verdicts;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>parallel mapper</b> demo — the same check over everything he got hold of.
 */
public final class ParallelMapperPattern {

    private ParallelMapperPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        // The mapper collects each per-item invocation under the agent's outputKey, and binds
        // the item itself to the sub-agent's first argument.
        var check = AgenticServices.agentBuilder(FoodSafetyCheck.class)
                .chatModel(model)
                .name("FoodSafetyCheck")
                .outputKey(Verdict.class)
                .build();
        UntypedAgent app = AgenticServices.parallelMapperBuilder()
                .subAgents(check)
                .itemsProvider(new Eaten().name())
                .outputKey(Verdicts.class)
                .listener(listener)
                .build();
        // The items come from what the user typed (comma- or semicolon-separated), not a
        // hard-coded list — otherwise the input box on the page has no effect here.
        List<String> eaten = items(input);
        var r = app.invokeWithAgenticScope(Map.of(new Eaten().name(), eaten));
        // Each verdict is paired back with the item it is about. The mapper preserves
        // order, and String.valueOf(List) would put five unlabelled verdicts on the screen
        // for the room to match up by counting — the moment the demo loses them.
        //
        // Note what the typed key bought: Verdicts is a TypedKey<List<String>>, so this
        // reads as a List with no cast and no instanceof. The string version of this line
        // returned Object and had to be interrogated at run time.
        var scope = r.agenticScope();
        List<String> said = scope == null ? List.of()
                : requireNonNullElse(scope.readState(Verdicts.class), List.<String>of());
        if (!said.isEmpty()) {
            return IntStream.range(0, said.size())
                    .mapToObj(i -> "- **" + (i < eaten.size() ? eaten.get(i) : "item " + i)
                            + "** — " + said.get(i))
                    .collect(joining("\n"));
        }
        return String.valueOf(r.result());
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The agent is drawn as a stack: one agent, invoked once per item, all at once. A
        // single box would say "one call", which is the opposite of what a mapper does.
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "he ate", "input").withSub("5 items"),
                        node("check", "FoodSafetyCheck", "agent")
                                .withSub("once per item").asStack(),
                        node("gather", "gather", "join").withSub("one verdict each")),
                List.of(edge("in", "check", "scatter"),
                        edge("check", "gather", "verdicts")));

        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "The picnic. Five things off the blanket before anybody noticed.",
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
                ParallelMapperPattern::run);
    }
}
