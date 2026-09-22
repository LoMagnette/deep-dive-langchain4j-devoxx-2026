package dev.devoxx.dashboard.demos._05_parallelmapper;

import static dev.devoxx.dashboard.catalog.Topology.edge;
import static dev.devoxx.dashboard.catalog.Topology.graph;
import static dev.devoxx.dashboard.catalog.Topology.node;
import static dev.devoxx.dashboard.support.Parsing.items;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.IntStream;

import dev.devoxx.dashboard.catalog.PatternDef;
import dev.devoxx.dashboard.catalog.Topology;
import dev.devoxx.dashboard.run.CurrentRun;
import dev.devoxx.dashboard.run.StreamingListener;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Wiring for the <b>parallel mapper</b> demo — the same check over everything the beard held.
 */
public final class ParallelMapperPattern {

    private ParallelMapperPattern() {
    }

    /** The wiring. Everything below it is the dashboard telling itself how to draw this. */
    static String run(ChatModel model, String input, StreamingListener listener) {
        CurrentRun.set(listener);
        ParallelMapperSystem app;
        try {
            app = AgenticServices.createAgenticSystem(ParallelMapperSystem.class, model);
        } finally {
            CurrentRun.clear();
        }
        // The items come from what the user typed (comma- or semicolon-separated), not a
        // hard-coded list — otherwise the input box on the page has no effect here.
        List<String> found = items(input);
        // Typed all the way out: the mapper's gathered verdicts arrive as a List<String> with
        // no cast and no scope read, and the order matches the items that produced them.
        List<String> said = requireNonNullElse(app.check(found), List.<String>of());
        if (said.isEmpty()) {
            return "nothing came back";
        }
        return IntStream.range(0, said.size())
                .mapToObj(i -> "- **" + (i < found.size() ? found.get(i) : "item " + i)
                        + "** — " + said.get(i))
                .collect(joining("\n"));
    }

    /** How the page draws it, and what the catalogue shows. */
    public static PatternDef define() {
        // The agent is drawn as a stack: one agent, invoked once per item, all at once. A
        // single box would say "one call", which is the opposite of what a mapper does.
        Topology.Graph topo = graph("fanout",
                List.of(node("in", "the beard", "input").withSub("5 items"),
                        node("check", "BeardOverflow", "agent")
                                .withSub("once per item").asStack(),
                        node("gather", "gather", "join").withSub("one verdict each")),
                List.of(edge("in", "check", "scatter"),
                        edge("check", "gather", "verdicts")));

        return new PatternDef("parallelMapper", "Parallel Mapper", "workflow",
                "A Bouvier's beard is a collection type. This is one walk's worth, emptied "
                        + "onto the kitchen table. Nobody knows about the conker.",
                null,
                "Map one agent over a collection in parallel (scatter/gather). Whatever came "
                        + "out of the beard, one verdict each. The width of the fan-out is data, "
                        + "decided at run time — and you already know all five answers, so you "
                        + "can mark this run yourself.",
                "Beware fan-out cost and rate limits when the list is long — this is the pattern "
                        + "where emptying a whole beard into the box quietly becomes fifty "
                        + "concurrent calls.",
                topo,
                "a cooked chicken bone; half a croissant; one conker; somebody's left glove; "
                        + "and roughly a litre of yesterday's puddle",
                ParallelMapperPattern::run);
    }
}
